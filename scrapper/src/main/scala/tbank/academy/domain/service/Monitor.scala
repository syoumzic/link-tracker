package tbank.academy.domain.service

import cats.effect.Async
import cats.effect.implicits.genSpawnOps
import cats.effect.kernel.Resource
import cats.implicits._
import tbank.academy.config.AppConfig
import tbank.academy.domain.client.{BotClient, GithubClient, StackoverflowClient}
import tbank.academy.domain.repository.LinkRepository
import tbank.academy.{Github, Stackoverflow}
import tofu.WithContext

trait Monitor[F[_]] {
  def run: F[Unit]
}

object Monitor {
  def pooling[F[_]: Async](
      linkRepository: LinkRepository[F],
      botClient: BotClient[F],
      githubClient: GithubClient[F],
      stackoverflowClient: StackoverflowClient[F]
  )(
      implicit context: WithContext[F, AppConfig]
  ): Resource[F, Monitor[F]] = for {
    monitor <- Resource.pure(make(linkRepository, botClient, githubClient, stackoverflowClient))
    _       <- monitor.run.foreverM.background.void
  } yield monitor

  def make[F[_]: Async](
      linkRepository: LinkRepository[F],
      botClient: BotClient[F],
      githubClient: GithubClient[F],
      stackoverflowClient: StackoverflowClient[F]
  )(implicit
      context: WithContext[F, AppConfig]
  ): Monitor[F] = new Monitor[F] {
    def run: F[Unit] = for {
      timeout      <- context.ask(_.monitor.timeout)
      links        <- linkRepository.getLinks
      updatedLinks <- links.traverse(link =>
        link.site match {
          case Github        => githubClient.requestUpdate(link, timeout)
          case Stackoverflow => stackoverflowClient.requestUpdate(link, timeout)
        }
      ).map(_.flatten)
      _ <- linkRepository.updateLinks(updatedLinks)
      _ <- updatedLinks.traverse(botClient.updateLink)
    } yield ()
  }
}
