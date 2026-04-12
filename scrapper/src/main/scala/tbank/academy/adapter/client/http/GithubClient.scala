package tbank.academy.adapter.client.http

import cats.effect.Temporal
import cats.implicits._
import io.circe.generic.auto._
import sttp.capabilities.fs2.Fs2Streams
import sttp.client4.circe.asJson
import sttp.client4.{StreamBackend, basicRequest}
import sttp.model.Uri
import tbank.academy.Link
import tbank.academy.domain.client.GithubClient
import tofu.logging.LoggingCompanion
import tofu.syntax.location.logging._

import scala.concurrent.duration.FiniteDuration

object GithubClient extends LoggingCompanion[GithubClient] {

  def make[F[_]: StackoverflowClient.Log](
      client: StreamBackend[F, Fs2Streams[F]]
  )(implicit temporal: Temporal[F]): GithubClient[F] =
    (link: Link, timeout: FiniteDuration) =>
      basicRequest
        .get(Uri.unsafeApply(link.url))
        .response(asJson[GithubRequest])
        .send(client)
        .flatMap(_.body match {
          case Right(GithubRequest(updatedAt)) =>
            link.lastUpdate
              .filter(_.isBefore(updatedAt))
              .as(link.copy(lastUpdate = Some(updatedAt)))
              .pure[F]
          case Left(error) => errorCause"Decode error" (error) >> Option.empty[Link].pure[F]
        })
        .flatTap(_ => temporal.sleep(timeout))
}
