package tbank.academy.domain.service

import cats.effect.implicits._
import cats.effect.{Async, Resource}
import cats.implicits._
import fs2.Stream
import tbank.academy.{DomainError, Stackoverflow}
import tbank.academy.adapter.client.http.Domain.{AnswerItem, CommentItem, QuestionItem}
import tbank.academy.config.AppConfig
import tbank.academy.domain.client.{BotClient, StackoverflowBatchClient}
import tbank.academy.domain.repository.LinkRepository
import tofu.WithContext
import tofu.logging.LoggingCompanion
import tbank.academy.Predef._

import java.time.{Instant, ZoneId, ZonedDateTime}

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

      service = makeInternal(client, botClient, linkRepository)(maxConcurrent)

      _ <- service.update.foreverM.background.void
    } yield service

  def makeInternal[F[_]: StackoverflowAnalyzer.Log](
      client: StackoverflowBatchClient[F],
      botClient: BotClient[F],
      linkRepository: LinkRepository[F]
  )(maxConcurrent: Int)(implicit async: Async[F]): StackoverflowAnalyzer[F] =
    new StackoverflowAnalyzer[F] {
      override def update: F[Unit] =
        Stream
          .evalSeq(linkRepository.getLinks)
          .filter(_.site == Stackoverflow)
          .parEvalMap(maxConcurrent)(link =>
            getQuestion(link.apiUrl)
              .flatMap(question =>
                (Stream.evalSeq(client.answer(link.apiUrl)) ++ Stream.evalSeq(client.comments(link.apiUrl)))
                  .compile
                  .toVector
                  .map(
                    _.sortBy(_.creationDate)
                      .drop(link.processedCount)
                      .map {
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
                      .size
                  )
                  .flatMap(changed =>
                    async.whenA(changed > 0)(linkRepository.updateCount(link.url, link.processedCount + changed))
                  )
              )
          )
          .compile
          .drain

      private def getQuestion(url: String): F[QuestionItem] =
        client.questions(url)
          .flatMap(_.headOption.liftTo[F](NoQuestionFound))

      private def ofEpochTime(timestamp: Long): ZonedDateTime = ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(timestamp),
        ZoneId.of("UTC")
      )
    }
}
