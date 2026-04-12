package tbank.academy.http

import tbank.academy.Link

case class ApiErrorResponse(
    description: String,
    code: String,
    exceptionName: String,
    exceptionMessage: String,
    stacktrace: List[String]
)

case class LinkResponse(id: Long, uri: String, tags: Set[String], filters: Set[String])

object LinkResponse {
  def apply(chatId: Long, link: Link): LinkResponse = LinkResponse(
    id = chatId,
    uri = link.url,
    tags = link.tags,
    filters = Set.empty
  )
}

case class AddLinkRequest(link: String, tags: Set[String], filters: Set[String])

case class RemoveLinkRequest(link: String)

case class ListLinksResponse(links: List[LinkResponse], size: Int)

object ListLinksResponse {
  def apply(chatId: Long, links: List[Link]): ListLinksResponse = ListLinksResponse(
    links = links.map(LinkResponse.apply(chatId, _)),
    size = links.size
  )
}

case class LinkUpdate(id: Long, uri: String, description: String, tgChatIds: Set[Long])

case class GetListLinksRequest(tag: Option[String])

object LinkUpdate {
  def apply(link: Link): LinkUpdate = LinkUpdate(
    id = 0,
    uri = link.url,
    description = "update link",
    tgChatIds = link.chatIds
  )
}
