package tbank.academy.adapter.persistance.slickDB

import cats.effect.Async
import cats.implicits._
import doobie.postgres.sqlstate
import org.postgresql.util.PSQLException
import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import tbank.academy.domain.{repository => domain}
import tbank.academy.adapter.persistance.slickDB.Domain._
import slick.jdbc.PostgresProfile.api._
import tbank.academy.domain.repository.ChatRepository.{ChatAlreadyExist, ChatNotFound}

object ChatRepository {
  def make[F[_]: Async](database: Database): domain.ChatRepository[F] = new domain.ChatRepository[F] {
    private def runQuery[T](action: DBIO[T]): F[T] =
      Async[F].fromFuture(Async[F].delay(database.run(action)))

    def registerChat(chatId: Long): F[Unit] = {
      val action = chats += chatId

      runQuery(action).void.adaptError {
        case e: PSQLException if e.getSQLState == sqlstate.class23.UNIQUE_VIOLATION.value =>
          ChatAlreadyExist(chatId)
      }
    }

    def deleteChat(chatId: Long): F[Unit] = {
      val action = chats.filter(_.chatId === chatId).delete

      runQuery(action).flatMap { rowsAffected =>
        if (rowsAffected == 1) Async[F].unit
        else Async[F].raiseError(ChatNotFound(chatId))
      }
    }
  }
}
