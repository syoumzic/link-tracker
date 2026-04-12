package tbank.academy.domain.repository

import derevo.derive
import tbank.academy.{DomainError, Link}
import tofu.higherKind.derived.representableK
import tofu.logging.derivation.loggingMid

@derive(representableK, loggingMid)
trait LinkRepository[F[_]] {
  def insertLink(chatId: Long, link: Link): F[Link]
  def getLinks(chatId: Long): F[List[Link]]
  def getLinks(chatId: Long, tag: String): F[List[Link]]
  def getLinks: F[List[Link]]
  def updateLinks(links: List[Link]): F[Unit]
  def deleteLink(chatId: Long, url: String): F[Link]
}

object LinkRepository {
  sealed trait Error extends DomainError

  case object LinkNotFound extends Error
}
