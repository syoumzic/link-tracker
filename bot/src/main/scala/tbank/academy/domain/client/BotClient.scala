package tbank.academy.domain.client

import canoe.api.Bot
import canoe.models.{BotCommand, Chat}

trait BotClient[F[_]] {
  def setMyCommands(commands: List[BotCommand]): F[Unit]
  def sendMessage(chat: Chat, message: String): F[Unit]
  def pooling: Bot[F]
}
