package tbank.academy.http

import tbank.academy.Link
import derevo.derive
import derevo.tethys.{tethysReader, tethysWriter}

import java.time.ZonedDateTime

@derive(tethysReader, tethysWriter)
case class ApiErrorResponse(
    description: String,
    code: String,
    exceptionName: String,
    exceptionMessage: String,
    stacktrace: List[String]
)

@derive(tethysReader, tethysWriter)
case class LinkResponse(id: Long, uri: String, tags: Set[String], filters: Set[String])

object LinkResponse {
  def apply(chatId: Long, link: Link): LinkResponse = LinkResponse(
    id = chatId,
    uri = link.url,
    tags = link.tags,
    filters = Set.empty
  )
}

@derive(tethysWriter, tethysReader)
case class AddLinkRequest(link: String, tags: Set[String], filters: Set[String])

@derive(tethysWriter, tethysReader)
case class RemoveLinkRequest(link: String)

@derive(tethysReader, tethysWriter)
case class ListLinksResponse(links: List[LinkResponse], size: Int)

object ListLinksResponse {
  def apply(chatId: Long, links: List[Link]): ListLinksResponse = ListLinksResponse(
    links = links.map(LinkResponse.apply(chatId, _)),
    size = links.size
  )
}

@derive(tethysWriter, tethysReader)
case class LinkUpdate(id: Long, uri: String, description: String, tgChatIds: Set[Long])

@derive(tethysWriter, tethysReader)
case class GetListLinksRequest(tag: Option[String])

object LinkUpdate {
  def apply(link: Link): LinkUpdate = LinkUpdate(
    id = 0,
    uri = link.url,
    description = "update link",
    tgChatIds = link.chatIds
  )

  @derive(tethysWriter, tethysReader)
  case class UpdatePullRequest(
      chatIds: Set[Long],
      url: String,
      title: String,
      username: String,
      uptime: ZonedDateTime
  )

  @derive(tethysWriter, tethysReader)
  case class UpdateIssue(
      chatIds: Set[Long],
      url: String,
      title: String,
      username: String,
      uptime: ZonedDateTime,
      description: String
  )

  @derive(tethysWriter, tethysReader)
  case class UpdateQuestion(
      chatIds: Set[Long],
      question: String,
      username: String,
      uptime: ZonedDateTime,
      description: String
  )
}
