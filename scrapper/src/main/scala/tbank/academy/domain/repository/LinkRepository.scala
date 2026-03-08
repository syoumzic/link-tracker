package tbank.academy.domain.repository

import cats.effect.{Async, Ref}
import cats.implicits._
import tbank.academy.domain.model.{Link, TgChat}

trait LinkRepository[F[_]] {
  def getLinks(id: TgChat.Id): F[List[Link]]
  def getLinks: F[List[Link]]
  def updateLinks(links: List[Link]): F[Unit]
}

object LinkRepository {
  def inMemory[F[_]: Async](links: Ref[F, List[Link]]): LinkRepository[F] = new LinkRepository[F] {
    override def getLinks(id: TgChat.Id): F[List[Link]] = links.get.map(_.filter(_.chatIds.contains(id)))

    override def getLinks: F[List[Link]] = links.get

    override def updateLinks(updates: List[Link]): F[Unit] =
      links.update(_.map(link => updates.find(_.uri == link.uri).getOrElse(link)))
  }
}
