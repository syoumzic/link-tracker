package tbank.academy.domain.telegram.command

import canoe.api._
import tbank.academy.domain.client.TgClient
import tbank.academy.domain.telegram.command.Command.privateChat
import tofu.logging._
import tofu.syntax.logging._

trait StartCommand[F[_]] extends Command[F]

object StartCommand extends LoggingCompanion[StartCommand] {
  def make[F[_]: StartCommand.Log](client: TgClient[F]): StartCommand[F] = new StartCommand[F] {
    override val name: String        = "/start"
    override val description: String = "Вывести преветственное сообщение"

    override def scenario: Scenario[F, Unit] = for {
      chat <- Scenario.expect(privateChat(name))
      _    <- Scenario.eval(info"в чате ${chat.id} воспользовались командой /start")
      _    <- Scenario.eval(client.sendMessage(
        chat.id,
        "Добро пожаловать! Чтобы ознакомиться с тем, что я умею введите /help"
      ))
    } yield ()
  }
}
