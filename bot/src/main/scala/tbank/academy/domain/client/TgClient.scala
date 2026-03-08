package tbank.academy.domain.client

import canoe.api.Bot
import canoe.models.{BotCommand, Chat}
import tbank.academy.domain.model.TgChat

trait TgClient[F[_]] {
  def setMyCommands(commands: List[BotCommand]): F[Unit]
  def sendMessage(chat: Chat, message: String): F[Unit]
  def updateLink(chat: TgChat.Id, uri: String): F[Unit]
  def pooling: Bot[F]
}
