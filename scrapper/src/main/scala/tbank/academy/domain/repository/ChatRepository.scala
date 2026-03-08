package tbank.academy.domain.repository

import cats.MonadThrow
import cats.effect.{Concurrent, Ref}
import derevo.derive
import tbank.academy.domain.model.{DomainError, Link, TgChat}
import tofu.higherKind.derived.representableK
import tofu.logging.derivation.loggingMid
import tofu.logging.{Logging, LoggingCompanion}
import cats.implicits._
import com.softwaremill.quicklens.ModifyPimp
import tbank.academy.domain.model.TgChat.Id

@derive(representableK, loggingMid)
trait ChatRepository[F[_]] {
  def registerChat(chatId: TgChat.Id): F[Unit]
  def deleteChat(chatId: TgChat.Id): F[Unit]
  def getLinks(chatId: TgChat.Id, tag: Option[Link.Tag]): F[List[Link]]
  def addLink(chatId: TgChat.Id, link: Link): F[Link]
  def deleteLink(chatId: TgChat.Id, uri: String): F[Link]
}

object ChatRepository extends LoggingCompanion[ChatRepository] {
  sealed trait Error extends DomainError

  case class ChatAlreadyExist(id: TgChat.Id) extends Error

  case class ChatNotFound(id: TgChat.Id) extends Error

  case class LinkNotFound(id: TgChat.Id, link: String) extends Error
  def inMemory[F[_]: Concurrent: Logging.Make](
      chats: Ref[F, Map[TgChat.Id, TgChat]],
      links: Ref[F, List[Link]]
  )(implicit R: MonadThrow[F]): ChatRepository[F] =
    new ChatRepository[F] { self =>
      override def registerChat(chatId: TgChat.Id): F[Unit] =
        chats
          .get
          .flatMap(chatMap =>
            if (chatMap.contains(chatId)) R.raiseError(ChatAlreadyExist(chatId))
            else chats.update(_.updated(chatId, TgChat(chatId)))
          )

      override def deleteChat(chatId: TgChat.Id): F[Unit] =
        chats
          .get
          .flatMap(chatMap =>
            if (chatMap.contains(chatId))
              chats.update(_.removed(chatId)) >>
                links.update(_.map(_.modify(_.chatIds).using(_.filterNot(_ == chatId))))
            else R.raiseError(ChatNotFound(chatId))
          )

      override def getLinks(chatId: TgChat.Id, tag: Option[Link.Tag]): F[List[Link]] =
        chats
          .get
          .flatMap(
            _.get(chatId)
              .map(_.links.filter(_.tags.contains(tag)).pure[F])
              .getOrElse(R.raiseError(ChatNotFound(chatId)))
          )

      override def deleteLink(chatId: TgChat.Id, rawUri: String): F[Link] = {
        links
          .get
          .flatMap(
            _.find(_.uri.pathToString == rawUri)
              .map(deletedLink =>
                links.update(_.filterNot(_ == deletedLink)) >>
                  chats.update(_.view.mapValues(_.modify(_.links).using(_.filterNot(_ == deletedLink))).toMap) >>
                  deletedLink.pure[F]
              )
              .getOrElse(R.raiseError(LinkNotFound(chatId, rawUri)))
          )
      }

      override def addLink(chatId: Id, link: Link): F[Link] =
        chats
          .get
          .flatMap(
            _.get(chatId)
              .map(chat => chats.update(_.updated(chatId, chat.modify(_.links).using(link :: _))) >> link.pure[F])
              .getOrElse(R.raiseError(ChatNotFound(chatId)))
          )
    }
}
