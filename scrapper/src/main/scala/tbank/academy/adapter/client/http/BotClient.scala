package tbank.academy.adapter.client.http

import cats.effect.Async
import cats.implicits._
import glass.Extract
import io.circe.generic.auto._
import sttp.capabilities.fs2.Fs2Streams
import sttp.client4.circe._
import sttp.client4.{StreamBackend, basicRequest, ignore}
import sttp.model.Uri
import tbank.academy.config.AppConfig
import tbank.academy.config.AppConfig.BotConfig
import tbank.academy.domain.client.BotClient
import tbank.academy.domain.model.Link
import tbank.academy.domain.model.http.LinkUpdate
import tofu.WithContext

object BotClient {
  def make[F[_]: Async](
      client: StreamBackend[F, Fs2Streams[F]]
  )(implicit C: WithContext[F, AppConfig]): BotClient[F] =
    (link: Link) =>
      url.flatMap(url =>
        basicRequest
          .post(url)
          .body(asJson(LinkUpdate(link)))
          .response(ignore)
          .send(client)
          .void
      )

  implicit val stackOverflowConfigExtractor: Extract[AppConfig, BotConfig] = _.bot

  private def url[F[_]](implicit
      C: WithContext[F, AppConfig],
      u: Extract[AppConfig, BotConfig]
  ): F[Uri] =
    WithContext[F, AppConfig].extract(u).ask(_.uri)
}
