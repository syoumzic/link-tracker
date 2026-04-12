package tbank.academy.adapter.client.http

import cats.effect.Async
import cats.implicits._
import sttp.capabilities.fs2.Fs2Streams
import sttp.client4.tethysJson.asJson
import sttp.client4.{StreamBackend, basicRequest, ignore}
import sttp.model.Uri
import tbank.academy.Link
import tbank.academy.config.AppConfig
import tbank.academy.domain.client.BotClient
import tbank.academy.http.LinkUpdate
import tbank.academy.http.LinkUpdate._
import tofu.WithContext
import tethys.jackson.jacksonTokenWriterProducer

import java.time.ZonedDateTime

object BotClient {
  def make[F[_]: Async](client: StreamBackend[F, Fs2Streams[F]])(implicit
      context: WithContext[F, AppConfig]
  ): F[BotClient[F]] = for {
    baseUrl            <- context.ask(_.bot.url)
    maxDescriptionSize <- context.ask(_.bot.maxDescriptionSize)
  } yield internalMake(client)(baseUrl, maxDescriptionSize)

  private def internalMake[F[_]: Async](
      client: StreamBackend[F, Fs2Streams[F]]
  )(botUrl: Uri, maxDescriptionSize: Int): BotClient[F] = new BotClient[F] {
    // deprecated
    override def updatePost(link: Link): F[Unit] =
      basicRequest
        .post(botUrl.addPath("updateLink"))
        .body(asJson(LinkUpdate(link)))
        .response(ignore)
        .send(client)
        .void

    override def updatePullRequest(
        chatIds: Set[Long],
        url: String,
        title: String,
        username: String,
        uptime: ZonedDateTime,
    ): F[Unit] =
      basicRequest
        .post(botUrl.addPath("updatePullRequest"))
        .body(asJson(UpdatePullRequest(chatIds, url, title, username, uptime)))
        .response(ignore)
        .send(client)
        .void

    override def updateIssue(
        chatIds: Set[Long],
        url: String,
        title: String,
        username: String,
        uptime: ZonedDateTime,
        description: String
    ): F[Unit] =
      basicRequest
        .post(botUrl.addPath("updateIssue"))
        .body(asJson(UpdateIssue(chatIds, url, title, username, uptime, description.take(maxDescriptionSize))))
        .response(ignore)
        .send(client)
        .void

    override def updateComment(
        chatIds: Set[Long],
        question: String,
        username: String,
        uptime: ZonedDateTime,
        description: String
    ): F[Unit] =
      basicRequest
        .post(botUrl.addPath("updateComment"))
        .body(asJson(UpdateQuestion(chatIds, question, username, uptime, description.take(maxDescriptionSize))))
        .response(ignore)
        .send(client)
        .void

    override def updateAnswer(
        chatIds: Set[Long],
        question: String,
        username: String,
        uptime: ZonedDateTime,
        description: String
    ): F[Unit] =
      basicRequest
        .post(botUrl.addPath("updateAnswer"))
        .body(asJson(UpdateQuestion(chatIds, question, username, uptime, description.take(maxDescriptionSize))))
        .response(ignore)
        .send(client)
        .void
  }
}
