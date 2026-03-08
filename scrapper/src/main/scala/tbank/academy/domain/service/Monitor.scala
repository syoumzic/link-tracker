package tbank.academy.domain.service

import cats.effect.Async
import cats.effect.implicits.genSpawnOps
import cats.effect.kernel.Resource
import cats.implicits._
import tbank.academy.domain.client.{BotClient, Crawler}
import tbank.academy.domain.model.{Link, Site}
import tbank.academy.domain.repository.LinkRepository

trait Monitor[F[_]] {
  def run: F[Unit]
}

object Monitor {
  def pooling[F[_]: Async](
      linkRepository: LinkRepository[F],
      botClient: BotClient[F],
      crawlers: List[Crawler[F]]
  ): Resource[F, Monitor[F]] = for {
    monitor <- Resource.pure(make(linkRepository, botClient, crawlers))
    _       <- monitor.run.foreverM.background.void
  } yield monitor

  def make[F[_]: Async](
      linkRepository: LinkRepository[F],
      botClient: BotClient[F],
      crawlers: List[Crawler[F]]
  ): Monitor[F] = new Monitor[F] {
    def run: F[Unit] = for {
      links   <- linkRepository.getLinks
      updates <- crawlers.traverse(requestUpdated(_, links.groupBy(_.site))).map(_.flatten)
      _       <- linkRepository.updateLinks(updates)
      _       <- updates.traverse(botClient.updateLink)
    } yield ()

    private def requestUpdated(crawler: Crawler[F], links: Map[Site, List[Link]]): F[List[Link]] =
      links.getOrElse(crawler.site, List.empty[Link]).traverse(crawler.requestUpdate).map(_.flatten)
  }
}
