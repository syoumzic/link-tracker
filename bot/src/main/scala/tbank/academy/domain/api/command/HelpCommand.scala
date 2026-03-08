package tbank.academy.domain.api.command

import canoe.api._
import tbank.academy.domain.api.command.Command.privateChat
import tbank.academy.domain.client.BotClient
import tofu.logging.LoggingCompanion
import tofu.syntax.logging._

trait HelpCommand[F[_]] extends Command[F]

object HelpCommand extends LoggingCompanion[HelpCommand] {
  def make[F[_]: HelpCommand.Log](client: BotClient[F]): HelpCommand[F] =
    new HelpCommand[F] {
      override val name: String        = "/help"
      override val description: String = "Вывести help сообщение"

      def scenario: Scenario[F, Unit] = for {
        message <- Scenario.expect(privateChat(name))
        _       <- Scenario.eval(info"пользователь ${message.user} воспользовался командой /help")
        _       <- Scenario.eval(
          client.sendMessage(
            message.chat,
            """Я бот для агрегации уведомлений. Я умею:
              |/start - вывести преветственное сообщение
              |/help - вывести help сообщение""".stripMargin
          )
        )
      } yield ()
    }
}
