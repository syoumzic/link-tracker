package tbank.academy.domain.api.command

import canoe.api._
import tbank.academy.domain.api.command.Command.privateChat
import tbank.academy.domain.client.BotClient
import tofu.logging._
import tofu.syntax.logging._

trait StartCommand[F[_]] extends Command[F]

object StartCommand extends LoggingCompanion[StartCommand] {
  def make[F[_]: StartCommand.Log](client: BotClient[F]): StartCommand[F] = new StartCommand[F] {
    override val name: String        = "/start"
    override val description: String = "Вывести преветственное сообщение"

    override def scenario: Scenario[F, Unit] = for {
      message <- Scenario.expect(privateChat(name))
      _       <- Scenario.eval(info"пользователь ${message.user} воспользовался командой /start")
      _       <- Scenario.eval(client.sendMessage(
        message.chat,
        "Добро пожаловать! Чтобы ознакомиться с тем, что я умею введите /help"
      ))
    } yield ()
  }
}
