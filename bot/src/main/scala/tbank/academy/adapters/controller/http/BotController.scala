package tbank.academy.adapters.controller.http

import alleycats.std.all._
import cats.effect.Async
import cats.implicits._
import sttp.tapir.server.ServerEndpoint
import tbank.academy.adapters.controller.http.endpoints.BotEndpoints
import tbank.academy.domain.client.TgClient
import tbank.academy.http.Controller

trait BotController[F[_]] extends Controller[F] {
  def updatesPost: ServerEndpoint[Any, F]
}

object BotController {
  def make[F[_]: Async](apiClient: TgClient[F]): BotController[F] =
    new BotController[F] {
      override def updatesPost: ServerEndpoint[Any, F] =
        BotEndpoints.updatesPost
          .serverLogicSuccess { linkUpdate =>
            linkUpdate.tgChatIds.traverse_(apiClient.updateLink(_, linkUpdate.uri))
          }

      override val endpoints: List[ServerEndpoint[Any, F]] =
        List(updatesPost)
    }
}
