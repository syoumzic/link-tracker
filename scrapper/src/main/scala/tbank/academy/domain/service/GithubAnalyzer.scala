package tbank.academy.domain.service

import cats.effect.implicits._
import cats.effect.{Async, Resource}
import cats.implicits._
import fs2.Stream
import sttp.client4.ResponseException.{DeserializationException, UnexpectedStatusCode}
import tbank.academy.{Github, Link}
import tbank.academy.adapter.client.http.Domain.{GithubRequestItem, IssuesPayload, PullRequestPayload, events}
import tbank.academy.config.AppConfig
import tbank.academy.domain.client.{BotClient, GithubBatchClient}
import tbank.academy.domain.repository.LinkRepository
import tofu.WithContext
import tofu.logging.LoggingCompanion
import tofu.syntax.location.logging.LoggingInterpolator

import scala.concurrent.duration._

trait GithubAnalyzer[F[_]] {
  def update: F[Unit]
}

object GithubAnalyzer extends LoggingCompanion[GithubAnalyzer] {
  def make[F[_]: Async: GithubAnalyzer.Log](
      client: GithubBatchClient[F],
      botClient: BotClient[F],
      linkRepository: LinkRepository[F]
  )(implicit context: WithContext[F, AppConfig]): Resource[F, GithubAnalyzer[F]] =
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

      _ <- service.update.flatTap(_ => Async[F].sleep(delayMs.milliseconds)).foreverM.background.void
    } yield service

  def makeInternal[F[_]: GithubAnalyzer.Log](
      client: GithubBatchClient[F],
      botClient: BotClient[F],
      linkRepository: LinkRepository[F]
  )(maxConcurrent: Int, delayMs: Long)(implicit async: Async[F]): GithubAnalyzer[F] =
    new GithubAnalyzer[F] {
      override def update: F[Unit] =
        Stream
          .evalSeq(linkRepository.getLinks)
          .filter(_.site == Github)
          .parEvalMap(maxConcurrent)(link =>
            processLink(link).handleErrorWith {
              case e: UnexpectedStatusCode[?]  => debugCause"could not get ${link.apiUrl}" (e)
              case e: DeserializationException => debugCause"could not decode ${link.apiUrl}" (e)
              case e: Throwable                => debugCause"unexpected error processing ${link.apiUrl}" (e)
            }
          )
          .compile
          .drain
          .flatMap(_ => async.sleep(delayMs.millis))

      private def processLink(link: Link): F[Unit] =
        for {
          events  <- fetchAndFilterEvents(link)
          changed <- events
            .traverse_(processEvent(_, link))
            .as(events.size)
          _ <- async.whenA(changed > 0)(linkRepository.updateCount(link.url, link.processedCount + changed))
        } yield ()

      private def fetchAndFilterEvents(link: Link): F[Vector[GithubRequestItem]] =
        Stream
          .evalSeq(client.events(link.apiUrl))
          .filter(item =>
            item.payload match {
              case payload: PullRequestPayload => payload.action == events.opened
              case payload: IssuesPayload      => payload.action == events.opened
              case _                           => false
            }
          )
          .drop(link.processedCount)
          .compile
          .toVector

      private def processEvent(event: GithubRequestItem, link: Link): F[Unit] = event.payload match {
        case payload: PullRequestPayload =>
          botClient.updatePullRequest(
            chatIds = link.chatIds,
            url = link.url,
            title = payload.pullRequest.base.repo.name,
            username = event.actor.displayLogin,
            uptime = event.createdAt
          )
        case payload: IssuesPayload =>
          botClient.updateIssue(
            chatIds = link.chatIds,
            url = link.url,
            title = payload.issue.title,
            username = event.actor.displayLogin,
            uptime = event.createdAt,
            description = payload.issue.body
          )
        case _ => async.unit
      }
    }
}
