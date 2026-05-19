package tbank.academy.domain.client

import canoe.api.Bot
import canoe.models.BotCommand

trait TgClient[F[_]] {
  def setMyCommands(commands: List[BotCommand]): F[Unit]
  def sendMessage(chatId: Long, message: String): F[Unit]
  def pooling: Bot[F]
}
