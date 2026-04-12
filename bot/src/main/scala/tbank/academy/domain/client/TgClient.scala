package tbank.academy.domain.client

import canoe.api.Bot
import canoe.models.{BotCommand, Chat}

trait TgClient[F[_]] {
  def setMyCommands(commands: List[BotCommand]): F[Unit]
  def sendMessage(chat: Chat, message: String): F[Unit]
  def updateLink(chat: Long, uri: String): F[Unit]
  def pooling: Bot[F]
}
