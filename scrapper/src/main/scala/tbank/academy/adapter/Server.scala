package tbank.academy.adapter

import cats.effect._
import cats.effect.implicits._
import com.comcast.ip4s.{Host, Port}
import fs2.io.net.Network
import glass.Extract
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.{Router, Server}
import sttp.tapir.server.http4s.Http4sServerInterpreter
import tbank.academy.adapter.http.controller.Controller
import tbank.academy.config.AppConfig
import tbank.academy.config.AppConfig.ServerConfig
import tofu.WithContext
import tofu.logging.Logging
import tofu.syntax.logging._

object Server {
  def make[F[_]: Async: Network](botController: Controller[F])(implicit
      C: WithContext[F, AppConfig],
      L: Logging.Make[F]
  ): Resource[F, Server] = {
    implicit val serverLogging: Logging[F] = L.service[Server].asLogging
    for {
      host <- host.toResource
      port <- port.toResource

      routes = Http4sServerInterpreter[F]()
        .toRoutes(botController.endpoints)

      server <- EmberServerBuilder
        .default[F]
        .withHost(host)
        .withPort(port)
        .withHttpApp(Router("/" -> routes).orNotFound)
        .build
        .evalTap(server => info"Сервер доступен по адресу ${server.address.getHostName}:${server.address.getPort}")
    } yield server
  }

  implicit val serverExtractor: Extract[AppConfig, ServerConfig] = _.server

  private def host[F[_]](implicit
      C: WithContext[F, AppConfig],
      u: AppConfig Extract ServerConfig
  ): F[Host] =
    WithContext[F, AppConfig].extract(u).ask(_.host)

  private def port[F[_]](implicit
      C: WithContext[F, AppConfig],
      u: AppConfig Extract ServerConfig
  ): F[Port] =
    WithContext[F, AppConfig].extract(u).ask(_.port)
}
