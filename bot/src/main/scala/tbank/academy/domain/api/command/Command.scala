package tbank.academy.domain.api.command

import canoe.models.BotCommand
import canoe.syntax._
import tbank.academy.domain.api.handler.Handler
import tbank.academy.domain.model.{Message, User}

trait Command[F[_]] extends Handler[F] {
  val name: String
  val description: String
  def botCommand: BotCommand = BotCommand(name, description)
}

object Command {
  def privateChat(commandName: String): Expect[Message] =
    textMessage.startingWith(commandName)
      .andThen(Function.unlift(m =>
        m.from.map(from =>
          Message(m.chat, User(from))
        )
      ))
}
