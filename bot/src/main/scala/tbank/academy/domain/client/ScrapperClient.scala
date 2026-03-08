package tbank.academy.domain.client

import tbank.academy.domain.model.http.{LinkResponse, ListLinksResponse}
import tbank.academy.domain.model.{Link, TgChat}

trait ScrapperClient[F[_]] {
  def tgChatPost(chatId: TgChat.Id): F[Unit]
  def tgChatDelete(chatId: TgChat.Id): F[Unit]
  def linksGet(chatId: TgChat.Id, tag: Option[Link.Tag]): F[ListLinksResponse]
  def linksPost(chatId: TgChat.Id, url: String, tags: List[Link.Tag], filters: List[String]): F[LinkResponse]
  def linksDelete(chatId: TgChat.Id, url: String): F[LinkResponse]
}
