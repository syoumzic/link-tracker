package tbank.academy.domain.model

case class TgChat(id: TgChat.Id, links: List[Link] = List.empty)

object TgChat {
  type Id = Long
}
