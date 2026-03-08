package tbank.academy.wiring

import cats.effect.{Async, Resource}
import cats.implicits._
import fs2.io.net.Network
import org.http4s.server.Server
import tbank.academy.adapter.Server
import tbank.academy.config.AppConfig
import tbank.academy.domain.service._
import tofu.WithContext
import tofu.logging.Logging

case class Services[F[_]](server: Server, monitor: Monitor[F])

object Services {
  def make[F[_]: Async: Network: Logging.Make](
      repositories: Repositories[F],
      clients: Clients[F],
      controllers: Controllers[F]
  )(implicit C: WithContext[F, AppConfig]): Resource[F, Services[F]] = (
    Server.make[F](controllers.botController),
    Monitor.pooling[F](
      repositories.linkRepository,
      clients.botClient,
      List(clients.githubClient, clients.stackoverflowClient)
    )
  ).mapN(Services.apply)
}
