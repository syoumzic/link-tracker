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

trait TgService[F[_]] {
  def run: F[Unit]
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
  }
}
