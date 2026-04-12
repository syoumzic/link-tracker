package tbank.academy.wiring

import cats.effect.{Async, Resource}
import org.http4s.server.Server
import tbank.academy.adapters.Server
import tbank.academy.config.AppConfig
import tbank.academy.domain.telegram.TgService
import tofu.WithContext
import tofu.logging.Logging
import cats.implicits._
import fs2.io.net.Network

case class Services[F[_]](botService: TgService[F], server: Server)

object Services {
  def make[F[_]: Async: Network: Logging.Make](clients: Clients[F], controllers: Controllers[F])(
      implicit context: WithContext[F, AppConfig]
  ): Resource[F, Services[F]] =
    (
      TgService.pooling[F](clients.tgClient, clients.scrapperClient),
      Server.make[F](controllers.scrapperController)
    ).mapN(Services.apply)
}
