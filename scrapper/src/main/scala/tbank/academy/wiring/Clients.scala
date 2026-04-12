package tbank.academy.wiring

import cats.effect._
import cats.effect.implicits.effectResourceOps
import sttp.client4.httpclient.fs2.HttpClientFs2Backend
import tbank.academy.adapter.client.http.BotClient
import tbank.academy.config.AppConfig
import tbank.academy.domain.client._
import tofu.WithContext

case class Clients[F[_]](
    botClient: BotClient[F],
    githubClient: GithubBatchClient[F],
    stackoverflowClient: StackoverflowBatchClient[F]
)

object Clients {
  def make[F[_]: Async](implicit context: WithContext[F, AppConfig]): Resource[F, Clients[F]] = {
    for {
      client               <- HttpClientFs2Backend.resource[F]()
      githubClientTimeout  <- context.ask(_.clients.github.timeout).toResource
      stackoverflowTimeout <- context.ask(_.clients.stackoverflow.timeout).toResource

      githubClient        = ApiClient.make[F](client, githubClientTimeout)
      stackoverflowClient = ApiClient.make[F](client, stackoverflowTimeout)

      githubBatchClient        <- GithubBatchClient.make(githubClient).toResource
      stackoverflowBatchClient <- StackoverflowBatchClient.make(stackoverflowClient).toResource

      botClient <- BotClient.make(client).toResource
    } yield Clients(botClient, githubBatchClient, stackoverflowBatchClient)
  }
}
