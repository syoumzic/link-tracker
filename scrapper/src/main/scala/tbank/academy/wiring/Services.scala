package tbank.academy.wiring

import cats.effect.{Async, Resource}
import tbank.academy.domain.service._

case class Services[F[_]](chatService: ChatService[F], linkService: LinkService[F])

object Services {
  def make[F[_]: Async](
      repositories: Repositories[F]
  ): Resource[F, Services[F]] = Resource.pure(
    Services(
      ChatService.make(repositories.chatRepository),
      LinkService.make(repositories.linkRepository)
    )
  )
}
