package tbank.academy.domain.telegram.command

import canoe.models.messages.TextMessage
import canoe.models.{BotCommand, Chat}
import canoe.syntax._
import tbank.academy.domain.telegram.handler.Handler

trait Command[F[_]] extends Handler[F] {
  val name: String
  val description: String
  def botCommand: BotCommand = BotCommand(name, description)
}

object Command {
  def privateChat(commandName: String): Expect[Chat] =
    textMessage
      .startingWith(commandName)
      .andThen {
        case m: TextMessage => m.chat
      }
}
