package tbank.academy.adapter.persistance.doobieDB

import cats.effect.Async
import cats.implicits._
import doobie.implicits._
import doobie.postgres.sqlstate
import doobie.util.transactor.Transactor
import org.postgresql.util.PSQLException
import tbank.academy.domain.repository.ChatRepository.{ChatAlreadyExist, ChatNotFound}
import tbank.academy.domain.{repository => domain}

object ChatRepository {
  def make[F[_]](transactor: Transactor[F])(implicit async: Async[F]): domain.ChatRepository[F] =
    new domain.ChatRepository[F] {

      override def registerChat(chatId: Long): F[Unit] =
        sql"""INSERT INTO chats (chatId) VALUES ($chatId)"""
          .update
          .run
          .void
          .transact(transactor)
          .adaptError {
            case e: PSQLException if e.getSQLState == sqlstate.class23.UNIQUE_VIOLATION.value =>
              ChatAlreadyExist(chatId)
          }

      override def deleteChat(chatId: Long): F[Unit] =
        sql"""DELETE FROM chats WHERE chatId = $chatId"""
          .update
          .run
          .transact(transactor)
          .ensure(ChatNotFound(chatId))(_ == 1)
          .void
    }
}
