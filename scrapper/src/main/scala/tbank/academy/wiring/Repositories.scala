package tbank.academy.wiring

import cats.effect.{Async, Ref}
import cats.implicits._
import tbank.academy.domain.model.{Link, TgChat}
import tbank.academy.domain.repository.{ChatRepository, LinkRepository}
import tofu.logging.Logging

case class Repositories[F[_]](
    chatRepository: ChatRepository[F],
    linkRepository: LinkRepository[F]
)

object Repositories {
  def make[F[_]: Async: Logging.Make]: F[Repositories[F]] = for {
    chats <- Ref.of[F, Map[TgChat.Id, TgChat]](Map.empty)
    links <- Ref.of[F, List[Link]](List.empty)
  } yield Repositories(
    ChatRepository.inMemory[F](chats, links),
    LinkRepository.inMemory[F](links)
  )
}
