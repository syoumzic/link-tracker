package tbank.academy.domain.client

import tbank.academy.http._

trait ScrapperClient[F[_]] {
  def tgChatPost(chatId: Long): F[Unit]
  def tgChatDelete(chatId: Long): F[Unit]
  def linksGet(chatId: Long, tag: Option[String]): F[ListLinksResponse]
  def linksPost(chatId: Long, url: String, tags: Set[String], filters: Set[String]): F[LinkResponse]
  def linksDelete(chatId: Long, url: String): F[LinkResponse]
}
