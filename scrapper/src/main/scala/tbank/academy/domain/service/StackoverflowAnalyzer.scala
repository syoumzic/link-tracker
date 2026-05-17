package tbank.academy.domain.service

import cats.effect.implicits._
import cats.effect.{Async, Resource}
import cats.implicits._
import fs2.Stream
import sttp.client4.ResponseException.{DeserializationException, UnexpectedStatusCode}
import tbank.academy.{DomainError, Link, Stackoverflow}
import tbank.academy.adapter.client.http.Domain.{AnswerItem, CommentItem, Item, QuestionItem}
import tbank.academy.config.AppConfig
import tbank.academy.domain.client.{BotClient, StackoverflowBatchClient}
import tbank.academy.domain.repository.LinkRepository
import tofu.WithContext
import tofu.logging.LoggingCompanion
import tbank.academy.Predef._
import tofu.syntax.logging.LoggingInterpolator

import java.time.{Instant, ZoneId, ZonedDateTime}
import scala.concurrent.duration._

trait StackoverflowAnalyzer[F[_]] {
  def update: F[Unit]
}

object StackoverflowAnalyzer extends LoggingCompanion[StackoverflowAnalyzer] {
  sealed trait Error extends DomainError

  case object NoQuestionFound extends Error

  def make[F[_]: Async: StackoverflowAnalyzer.Log](
      client: StackoverflowBatchClient[F],
      botClient: BotClient[F],
      linkRepository: LinkRepository[F]
  )(implicit context: WithContext[F, AppConfig]): Resource[F, StackoverflowAnalyzer[F]] =
    for {
      maxConcurrent <-
        context
          .ask(_.analyzer.maxConcurrent.getOrElse(Runtime.getRuntime.availableProcessors()))
          .toResource
      delayMs <-
        context
          .ask(_.analyzer.delayMs)
          .toResource

      service = makeInternal(client, botClient, linkRepository)(maxConcurrent, delayMs)

      _ <- service.update.flatMap(_ => Async[F].sleep(delayMs.milliseconds)).foreverM.background.void
    } yield service

  def makeInternal[F[_]: StackoverflowAnalyzer.Log](
      client: StackoverflowBatchClient[F],
      botClient: BotClient[F],
      linkRepository: LinkRepository[F]
  )(maxConcurrent: Int, delayMs: Long)(implicit async: Async[F]): StackoverflowAnalyzer[F] =
    new StackoverflowAnalyzer[F] {
      override def update: F[Unit] =
        Stream
          .evalSeq(linkRepository.getLinks)
          .filter(_.site == Stackoverflow)
          .parEvalMap(maxConcurrent)(link =>
            processLink(link).recoverWith {
              case e: UnexpectedStatusCode[Any] => debugCause"could not get ${link.apiUrl}" (e)
              case e: DeserializationException  => debugCause"could not decode ${link.apiUrl}" (e)
              case NoQuestionFound              => debug"Неправильное состояние страницы ${link.apiUrl}"
            }
          )
          .compile
          .drain
          .flatMap(_ => async.sleep(delayMs.millis))

      private def processLink(link: Link): F[Unit] =
        for {
          question <- getQuestion(link.apiUrl)
          changed  <- fetchAndProcessItems(link, question)
          _        <- async.whenA(changed > 0)(linkRepository.updateCount(link.url, link.processedCount + changed))
        } yield ()

      private def fetchAndProcessItems(
          link: Link,
          question: QuestionItem
      ): F[Int] =
        for {
          items <- (Stream.evalSeq(client.answer(link.apiUrl)) ++ Stream.evalSeq(client.comments(link.apiUrl)))
            .compile
            .toVector
          count <- items
            .sortBy(_.creationDate)
            .drop(link.processedCount)
            .traverse_(processItem(_, link, question))
            .as(items.size)
        } yield count

      private def processItem(
          item: Item,
          link: Link,
          question: QuestionItem
      ): F[Unit] = item match {
        case item: AnswerItem => botClient.updateAnswer(
            chatIds = link.chatIds,
            question = question.title,
            username = item.owner.displayName,
            uptime = ofEpochTime(item.creationDate),
            description = item.body
          )
        case item: CommentItem => botClient.updateComment(
            chatIds = link.chatIds,
            question = question.title,
            username = item.owner.displayName,
            uptime = ofEpochTime(item.creationDate),
            description = item.body
          )
        case _ => async.unit
      }

      private def getQuestion(url: String): F[QuestionItem] =
        client.questions(url)
          .flatMap(_.headOption.liftTo[F](NoQuestionFound))

      private def ofEpochTime(timestamp: Long): ZonedDateTime = ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.of("UTC")
      )
    }
}
