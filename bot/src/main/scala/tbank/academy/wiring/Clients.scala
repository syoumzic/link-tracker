package tbank.academy.wiring

import cats.effect.Async
import cats.effect.kernel.Resource
import tbank.academy.config.AppConfig
import tbank.academy.domain.client.{ScrapperClient, TgClient}
import tofu.WithContext
import sttp.client4.httpclient.fs2.HttpClientFs2Backend
import tbank.academy.adapters.client.http.ScrapperClient
import tbank.academy.adapters.client.telegram.TgClient

case class Clients[F[_]](tgClient: TgClient[F], scrapperClient: ScrapperClient[F])

object Clients {
  def make[F[_]: Async](implicit C: WithContext[F, AppConfig]): Resource[F, Clients[F]] =
    for {
      http <- HttpClientFs2Backend.resource[F]()
      api  <- TgClient.make[F]
    } yield Clients(api, ScrapperClient.make[F](http))
}
