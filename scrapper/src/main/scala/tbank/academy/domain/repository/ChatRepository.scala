package tbank.academy.domain.repository

import derevo.derive
import tbank.academy.DomainError
import tofu.higherKind.derived.representableK
import tofu.logging.LoggingCompanion
import tofu.logging.derivation.loggingMid

@derive(representableK, loggingMid)
trait ChatRepository[F[_]] {
  def registerChat(chatId: Long): F[Unit]
  def deleteChat(chatId: Long): F[Unit]
}

object ChatRepository extends LoggingCompanion[ChatRepository] {
  sealed trait Error extends DomainError

  case class ChatAlreadyExist(id: Long) extends Error

  case class ChatNotFound(id: Long) extends Error
}
