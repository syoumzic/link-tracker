package tbank.academy.domain.api.handler

import canoe.api._
import canoe.models.messages.TextMessage
import canoe.syntax._
import tbank.academy.domain.client.BotClient

object NotFoundHandler {
  def make[F[_]](client: BotClient[F], commandNames: List[String]): Handler[F] = new Handler[F] {
    override val scenario: Scenario[F, Unit] = for {
      message <- Scenario.expect(incorrectCommand)
      _       <- Scenario.eval(client.sendMessage(
        message.chat,
        "Не совсем понимаю, что имелось ввиду. Cписок доступных комманд /help"
      ))
    } yield ()

    def incorrectCommand: Expect[TextMessage] =
      textMessage.when(message => !commandNames.contains(message.text))
  }
}
