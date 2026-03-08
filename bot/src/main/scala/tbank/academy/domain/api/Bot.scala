package tbank.academy.domain.api

import cats.effect._
import cats.implicits._
import fs2.Stream
import tbank.academy.domain.api.command.{Command, HelpCommand, StartCommand}
import tbank.academy.domain.api.handler.{Handler, NotFoundHandler}
import tbank.academy.domain.client.BotClient
import tofu.logging._
import tofu.syntax.logging._

trait Bot[F[_]] {
  def run: F[Unit]
}

object Bot extends LoggingCompanion[Bot] {
  def make[F[_]: Async: Bot.Log: Logging.Make](client: BotClient[F]): Bot[F] = new Bot[F] {
    val commands: List[Command[F]] = List(
      StartCommand.make[F](client),
      HelpCommand.make[F](client)
    )

    val handlers: List[Handler[F]] = NotFoundHandler.make[F](client, commands.map(_.name)) :: commands

    override def run: F[Unit] = {
      Stream.eval(info"бот начал свою работу")
        .evalTap(_ => client.setMyCommands(commands.map(_.botCommand).toList))
        .flatTap(_ => client.pooling.follow(handlers.map(_.scenario): _*))
        .onFinalize(info"бот своё отработал")
        .compile
        .drain
    }
  }
}
