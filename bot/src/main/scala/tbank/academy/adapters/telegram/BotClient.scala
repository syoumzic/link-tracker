package tbank.academy.adapters.telegram

import canoe.methods.commands.SetMyCommands
import canoe.models._
import cats.effect._
import tbank.academy.domain.client.BotClient
import cats.implicits._
import canoe.api._
import canoe.syntax._

object BotClient {
  def make[F[_]: Async](token: String): Resource[F, BotClient[F]] =
    TelegramClient(token).map(implicit client => make[F])

  private def make[F[_]: TelegramClient: Concurrent]: BotClient[F] = new BotClient[F](
  ) {
    override def setMyCommands(commands: List[BotCommand]): F[Unit] = SetMyCommands(commands).call.void

    override def sendMessage(chat: Chat, message: String): F[Unit] = chat.send(message).void

    override def pooling: Bot[F] = Bot.polling
  }
}
