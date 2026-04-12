package tbank.academy.adapters

import cats.effect._
import cats.effect.implicits._
import fs2.io.net.Network
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import sttp.tapir.server.http4s.Http4sServerInterpreter
import tbank.academy.adapters.controller.http.BotController
import tbank.academy.config.AppConfig
import tofu.WithContext
import tofu.logging.Logging
import tofu.syntax.logging._

object Server {
  def make[F[_]: Async: Network](scrapperController: BotController[F])(
      implicit
      context: WithContext[F, AppConfig],
      logging: Logging.Make[F]
  ): Resource[F, Server] = {
    implicit val serverLogging: Logging[F] = logging.service[Server].asLogging

    for {
      host <- context.ask(_.server.host).toResource
      port <- context.ask(_.server.port).toResource

      routes = Http4sServerInterpreter[F]()
        .toRoutes(scrapperController.endpoints)

      server <- EmberServerBuilder
        .default[F]
        .withHost(host)
        .withPort(port)
        .withHttpApp(Router("/" -> routes).orNotFound)
        .build
        .evalTap(server => info"Сервер доступен по адресу ${server.address.getHostName}:${server.address.getPort}")
    } yield server
  }
}
