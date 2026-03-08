package tbank.academy.adapter.client.http

import cats.effect.Async
import cats.implicits._
import glass.Extract
import io.circe.generic.auto._
import sttp.capabilities.fs2.Fs2Streams
import sttp.client4.circe.asJson
import sttp.client4.{StreamBackend, basicRequest}
import tbank.academy.config.AppConfig
import tbank.academy.config.AppConfig.Crawlers.GithubConfig
import tbank.academy.domain.client.GithubClient
import tbank.academy.domain.model.{Link, Site}
import tethys.JsonReader
import tofu.WithContext
import tofu.logging.LoggingCompanion
import tofu.syntax.location.logging._

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

object GithubClient extends LoggingCompanion[GithubClient] {
  case class GithubRequest(updatedAt: Instant)

  implicit val githubRequestReader: JsonReader[GithubRequest] =
    JsonReader.builder
      .addField[Instant]("updated_at")
      .buildReader(GithubRequest.apply)

  def make[F[_]: StackoverflowClient.Log](
      client: StreamBackend[F, Fs2Streams[F]]
  )(implicit A: Async[F], C: WithContext[F, AppConfig]): GithubClient[F] = new GithubClient[F] {
    override val site: Site = Site.Github

    def requestUpdate(link: Link): F[Option[Link]] =
      timeout
        .flatMap(A.sleep)
        .flatMap(_ =>
          basicRequest
            .get(link.uri)
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
        )

    implicit val stackOverflowConfigExtractor: Extract[AppConfig, GithubConfig] = _.crawlers.github

    private def timeout(implicit
        C: WithContext[F, AppConfig],
        u: AppConfig Extract GithubConfig
    ): F[FiniteDuration] =
      WithContext[F, AppConfig].extract(u).ask(_.timeout)
  }
}
