package tbank.academy.adapter.client.http

import cats.effect.Async
import cats.implicits._
import glass.Extract
import io.circe.generic.auto._
import sttp.capabilities.fs2.Fs2Streams
import sttp.client4.circe.asJson
import sttp.client4.{StreamBackend, basicRequest}
import tbank.academy.adapter.client.http.StackoverflowClient.StackoverflowGetRequest.Item
import tbank.academy.config.AppConfig
import tbank.academy.config.AppConfig.Crawlers.StackoverflowConfig
import tbank.academy.domain.client.StackoverflowClient
import tbank.academy.domain.model.{Link, Site}
import tbank.academy.domain.model.NEL._
import tofu.WithContext
import tofu.logging.LoggingCompanion
import tofu.syntax.location.logging._

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

object StackoverflowClient extends LoggingCompanion[StackoverflowClient] {
  case class StackoverflowGetRequest(items: NEL[Item])

  object StackoverflowGetRequest {
    case class Item(lastEditDate: Instant)
  }

  def make[F[_]: StackoverflowClient.Log](
      client: StreamBackend[F, Fs2Streams[F]]
  )(implicit A: Async[F], C: WithContext[F, AppConfig]): StackoverflowClient[F] = new StackoverflowClient[F] {
    override val site: Site = Site.StackOverflow

    override def requestUpdate(link: Link): F[Option[Link]] =
      timeout.flatMap(A.sleep)
        .flatMap(_ =>
          basicRequest
            .get(link.apiUrl)
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
        )

    implicit val stackOverflowConfigExtractor: Extract[AppConfig, StackoverflowConfig] = _.crawlers.stackoverflow

    private def timeout(implicit
        C: WithContext[F, AppConfig],
        u: AppConfig Extract StackoverflowConfig
    ): F[FiniteDuration] =
      WithContext[F, AppConfig].extract(u).ask(_.timeout)
  }
}
