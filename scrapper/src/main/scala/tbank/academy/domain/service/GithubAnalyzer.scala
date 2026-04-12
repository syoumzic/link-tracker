package tbank.academy.domain.service

import cats.effect.implicits._
import cats.effect.{Async, Resource}
import cats.implicits._
import fs2.Stream
import tbank.academy.Github
import tbank.academy.adapter.client.http.Domain.{IssuesPayload, PullRequestPayload, events}
import tbank.academy.config.AppConfig
import tbank.academy.domain.client.{BotClient, GithubBatchClient}
import tbank.academy.domain.repository.LinkRepository
import tofu.WithContext
import tofu.logging.LoggingCompanion

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

      service = makeInternal(client, botClient, linkRepository)(maxConcurrent)

      _ <- service.update.foreverM.background.void
    } yield service

  def makeInternal[F[_]: GithubAnalyzer.Log](
      client: GithubBatchClient[F],
      botClient: BotClient[F],
      linkRepository: LinkRepository[F]
  )(maxConcurrent: Int)(implicit async: Async[F]): GithubAnalyzer[F] =
    new GithubAnalyzer[F] {
      override def update: F[Unit] =
        Stream
          .evalSeq(linkRepository.getLinks)
          .filter(_.site == Github)
          .parEvalMap(maxConcurrent)(link =>
            Stream.evalSeq(client.events(link.apiUrl))
              .filter(requestItem =>
                requestItem.payload match {
                  case payload: PullRequestPayload => payload.action == events.opened
                  case payload: IssuesPayload      => payload.action == events.opened
                  case _                           => false
                }
              )
              .drop(link.processedCount)
              .evalTap(requestItem =>
                requestItem.payload match {
                  case payload: PullRequestPayload => botClient.updatePullRequest(
                      chatIds = link.chatIds,
                      url = link.url,
                      title = payload.pullRequest.base.repo.name,
                      username = requestItem.actor.displayLogin,
                      uptime = requestItem.createdAt
                    )
                  case payload: IssuesPayload => botClient.updateIssue(
                      chatIds = link.chatIds,
                      url = link.url,
                      title = payload.issue.title,
                      username = requestItem.actor.displayLogin,
                      uptime = requestItem.createdAt,
                      description = payload.issue.body
                    )
                  case _ => async.unit
                }
              )
              .compile
              .count
              .flatMap(changed =>
                async.whenA(changed > 0)(linkRepository.updateCount(link.url, link.processedCount + changed))
              )
          )
          .compile
          .drain
    }
}
