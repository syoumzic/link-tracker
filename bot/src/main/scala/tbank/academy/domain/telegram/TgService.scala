package tbank.academy.domain.telegram

import cats.effect._
import fs2.Stream
import tbank.academy.domain.client.{ScrapperClient, TgClient}
import tbank.academy.domain.telegram.command.{
  Command,
  HelpCommand,
  ListCommand,
  StartCommand,
  TrackCommand,
  UntrackCommand
}
import tbank.academy.domain.telegram.handler.{Handler, NotFoundHandler}
import tofu.logging._
import tofu.syntax.logging._
import cats.effect.implicits._
import cats.implicits._

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

trait TgService[F[_]] {
  def run: F[Unit]
  def updatePullRequest(chat: Long, url: String, title: String, username: String, uptime: ZonedDateTime): F[Unit]
  def updateIssue(
      chat: Long,
      url: String,
      title: String,
      username: String,
      uptime: ZonedDateTime,
      description: String
  ): F[Unit]
  def updateAnswer(chat: Long, question: String, username: String, uptime: ZonedDateTime, description: String): F[Unit]
  def updateComment(chat: Long, question: String, username: String, uptime: ZonedDateTime, description: String): F[Unit]
}

object TgService extends LoggingCompanion[TgService] {
  def pooling[F[_]: Async: Logging.Make](
      client: TgClient[F],
      scrapperClient: ScrapperClient[F]
  ): Resource[F, TgService[F]] = for {
    client <- Resource.pure(make[F](client, scrapperClient))
    _      <- client.run.background
  } yield client

  def make[F[_]: Async: TgService.Log: Logging.Make](
      client: TgClient[F],
      scrapperClient: ScrapperClient[F]
  ): TgService[F] = new TgService[F] {
    val commands: List[Command[F]] = List(
      StartCommand.make[F](client),
      HelpCommand.make[F](client),
      TrackCommand.make[F](client, scrapperClient),
      UntrackCommand.make[F](client, scrapperClient),
      ListCommand.make[F](client, scrapperClient)
    )

    private val timeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

    val handlers: List[Handler[F]] = NotFoundHandler.make[F](client, commands.map(_.name)) :: commands

    override def run: F[Unit] = {
      Stream.eval(info"бот начал свою работу")
        .evalTap(_ => client.setMyCommands(commands.map(_.botCommand)))
        .evalTap(_ => info"команды зарегистрированы в телеграмме")
        .flatTap(_ => client.pooling.follow(handlers.map(_.scenario): _*))
        .onFinalize(info"бот своё отработал")
        .compile
        .drain
    }

    override def updatePullRequest(
        chat: Long,
        url: String,
        title: String,
        username: String,
        uptime: ZonedDateTime
    ): F[Unit] = {
      val msg =
        s"""
           |🚀 <b>Pull Request обновлен</b>
           |
           |<b>Название:</b> $title
           |<b>Автор:</b> $username
           |<b>Время:</b> ${uptime.format(timeFormatter)}
           |
           |<a href="$url">Посмотреть изменения</a>
    """.stripMargin

      client.sendMessage(chat, msg)
    }

    override def updateIssue(
        chat: Long,
        url: String,
        title: String,
        username: String,
        uptime: ZonedDateTime,
        description: String
    ): F[Unit] = {
      val msg =
        s"""
           |📌 <b>Issue обновлена</b>
           |
           |<b>Название:</b> $title
           |<b>Автор:</b> $username
           |<b>Время:</b> ${uptime.format(timeFormatter)}
           |
           |<b>Описание:</b>
           |${description.take(200)}${if (description.length > 200) "..." else ""}
           |
           |<a href="$url">Открыть Issue</a>
    """.stripMargin

      client.sendMessage(chat, msg)
    }

    override def updateAnswer(
        chat: Long,
        question: String,
        username: String,
        uptime: ZonedDateTime,
        description: String
    ): F[Unit] = {
      val msg =
        s"""
           |✅ <b>Дан новый ответ</b>
           |
           |<b>Вопрос:</b> $question
           |<b>Ответил:</b> $username
           |<b>Время:</b> ${uptime.format(timeFormatter)}
           |
           |<b>Текст ответа:</b>
           |$description
    """.stripMargin

      client.sendMessage(chat, msg)
    }

    override def updateComment(
        chat: Long,
        question: String,
        username: String,
        uptime: ZonedDateTime,
        description: String
    ): F[Unit] = {
      val msg =
        s"""
           |💬 <b>Новый комментарий</b>
           |
           |<b>К вопросу:</b> $question
           |<b>Автор:</b> $username
           |<b>Время:</b> ${uptime.format(timeFormatter)}
           |
           |<b>Комментарий:</b>
           |$description
    """.stripMargin

      client.sendMessage(chat, msg)
    }
  }
}
