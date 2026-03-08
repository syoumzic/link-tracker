package tbank.academy.domain.telegram.command

import canoe.api._
import tbank.academy.domain.client.TgClient
import tbank.academy.domain.telegram.command.Command.privateChat
import tofu.logging.LoggingCompanion
import tofu.syntax.logging._

trait HelpCommand[F[_]] extends Command[F]

object HelpCommand extends LoggingCompanion[HelpCommand] {
  def make[F[_]: HelpCommand.Log](client: TgClient[F]): HelpCommand[F] =
    new HelpCommand[F] {
      override val name: String        = "/help"
      override val description: String = "Вывести help сообщение"

      def scenario: Scenario[F, Unit] = for {
        chat <- Scenario.expect(privateChat(name))
        _    <- Scenario.eval(info"в чате ${chat.id} воспользовались командой /help")
        _    <- Scenario.eval(
          client.sendMessage(
            chat,
            """Я бот для агрегации уведомлений. Я умею:
              |/start - вывести преветственное сообщение
              |/help - вывести help сообщение""".stripMargin
          )
        )
      } yield ()
    }
}
