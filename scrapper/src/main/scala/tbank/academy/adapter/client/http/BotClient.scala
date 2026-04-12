package tbank.academy.adapter.client.http

import cats.effect.Async
import cats.implicits._
import io.circe.generic.auto._
import sttp.capabilities.fs2.Fs2Streams
import sttp.client4.circe._
import sttp.client4.{StreamBackend, basicRequest, ignore}
import tbank.academy.Link
import tbank.academy.config.AppConfig
import tbank.academy.domain.client.BotClient
import tbank.academy.http.LinkUpdate
import tofu.WithContext

object BotClient {
  def make[F[_]: Async](
      client: StreamBackend[F, Fs2Streams[F]]
  )(implicit context: WithContext[F, AppConfig]): BotClient[F] =
    (link: Link) =>
      context.ask(_.bot.url).flatMap(url =>
        basicRequest
          .post(url)
          .body(asJson(LinkUpdate(link)))
          .response(ignore)
          .send(client)
          .void
      )
}
