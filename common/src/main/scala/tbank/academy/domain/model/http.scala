package tbank.academy.domain.model

object http {
  case class ApiErrorResponse(
      description: String,
      code: String,
      exceptionName: String,
      exceptionMessage: String,
      stacktrace: List[String]
  )

  case class LinkResponse(id: TgChat.Id, uri: String, tags: List[Link.Tag], filters: List[String])

  object LinkResponse {
    def apply(chatId: TgChat.Id, link: Link): LinkResponse = LinkResponse(
      id = chatId,
      uri = link.uri.pathToString,
      tags = link.tags,
      filters = Nil
    )
  }

  case class AddLinkRequest(link: String, tags: List[Link.Tag], filters: List[String])

  case class RemoveLinkRequest(link: String)

  case class ListLinksResponse(links: List[LinkResponse], size: Int)

  object ListLinksResponse {
    def apply(chatId: TgChat.Id, links: List[Link]): ListLinksResponse = ListLinksResponse(
      links = links.map(LinkResponse.apply(chatId, _)),
      size = links.size
    )
  }

  case class LinkUpdate(id: Long, uri: String, description: String, tgChatIds: List[Long])

  case class GetListLinksRequest(tag: Option[Link.Tag])

  object LinkUpdate {
    def apply(link: Link): LinkUpdate = LinkUpdate(
      id = link.uri.toString.hashCode.toLong,
      uri = link.uri.pathToString,
      description = s"${link.site.getClass.getSimpleName} link updated",
      tgChatIds = link.chatIds
    )
  }
}
