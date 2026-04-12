package tbank.academy.adapter.client.http

import cats.effect.{Async, Temporal}
import cats.implicits._
import io.circe.generic.auto._
import sttp.capabilities.fs2.Fs2Streams
import sttp.client4.circe.asJson
import sttp.client4.{StreamBackend, basicRequest}
import sttp.model.Uri
import tbank.academy.Link
import tbank.academy.domain.client.StackoverflowClient
import tofu.logging.LoggingCompanion
import tofu.syntax.location.logging._

import scala.concurrent.duration.FiniteDuration

object StackoverflowClient extends LoggingCompanion[StackoverflowClient] {
  def make[F[_]: StackoverflowClient.Log: Async](
      client: StreamBackend[F, Fs2Streams[F]]
  )(implicit temporal: Temporal[F]): StackoverflowClient[F] =
    (link: Link, timeout: FiniteDuration) =>
      basicRequest
        .get(Uri.unsafeApply(link.apiUrl))
        .response(asJson[StackoverflowGetRequest])
        .send(client)
        .flatMap(_.body match {
          case Right(stackoverflowGetQuestion) => stackoverflowGetQuestion.items.head.lastEditDate match {
              case lastSiteUpdate =>
                link.lastUpdate
                  .filter(_.isBefore(lastSiteUpdate))
                  .as(link.copy(lastUpdate = Some(lastSiteUpdate)))
                  .pure[F]
            }
          case Left(error) => errorCause"Decode error" (error) >> Option.empty[Link].pure[F]
        })
        .flatTap(_ => temporal.sleep(timeout))
}
