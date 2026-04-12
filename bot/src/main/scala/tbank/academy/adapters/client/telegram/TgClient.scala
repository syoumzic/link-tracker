package tbank.academy.adapters.client.telegram

import canoe.api._
import canoe.methods.commands.SetMyCommands
import canoe.models._
import canoe.syntax._
import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import tbank.academy.config.AppConfig
import tbank.academy.domain.client.TgClient
import tofu.WithContext

object TgClient {
  def make[F[_]: Async](implicit context: WithContext[F, AppConfig]): Resource[F, TgClient[F]] = {
    context.ask(_.bot.token).toResource
      .flatMap(token => TelegramClient.apply(token))
      .map(implicit client => application[F])
  }

  private def application[F[_]: TelegramClient: Concurrent]: TgClient[F] = new TgClient[F] {
    override def setMyCommands(commands: List[BotCommand]): F[Unit] = SetMyCommands(commands).call.void

    override def sendMessage(chat: Chat, message: String): F[Unit] = chat.send(message).void

    override def updateLink(chatId: Long, uri: String): F[Unit] =
      sendMessage(PrivateChat(chatId, None, None, None), s"Похоже по ссылке $uri что-то обновилось")

    override def pooling: Bot[F] = Bot.polling
  }
}
