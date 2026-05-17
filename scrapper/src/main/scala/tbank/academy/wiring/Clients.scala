package tbank.academy.wiring

import cats.effect._
import cats.effect.implicits.effectResourceOps
import sttp.client4.httpclient.fs2.HttpClientFs2Backend
import tbank.academy.adapter.client.http.BotClient
import tbank.academy.adapter.client.kafka.KafkaBotClient
import tbank.academy.adapter.kafka.KafkaProducerService
import tbank.academy.config.AppConfig
import tbank.academy.domain.client._
import tbank.academy.domain.client.BotClient
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

      kafkaEnabled <- context.ask(_.kafka.enabled).toResource
      botClient    <- if (kafkaEnabled) {
        for {
          kafkaProducer <- KafkaProducerService.make[F]
          kafkaBotClient = KafkaBotClient.make[F](kafkaProducer)
        } yield kafkaBotClient
      } else {
        BotClient.make(client).toResource
      }
    } yield Clients(botClient, githubBatchClient, stackoverflowBatchClient)
  }
}
