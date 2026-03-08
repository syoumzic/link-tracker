package tbank.academy.wiring

import cats.effect._
import sttp.client4.httpclient.fs2.HttpClientFs2Backend
import tbank.academy.adapter.client.http.{BotClient, GithubClient, StackoverflowClient}
import tbank.academy.config.AppConfig
import tbank.academy.domain.client._
import tofu.WithContext
import tofu.logging.Logging

case class Clients[F[_]](
    botClient: BotClient[F],
    githubClient: GithubClient[F],
    stackoverflowClient: StackoverflowClient[F]
)

object Clients {
  def make[F[_]: Async: Logging.Make](implicit C: WithContext[F, AppConfig]): Resource[F, Clients[F]] =
    HttpClientFs2Backend
      .resource[F]()
      .map(client =>
        Clients(
          BotClient.make[F](client),
          GithubClient.make[F](client),
          StackoverflowClient.make[F](client)
        )
      )
}
