package tbank.academy.domain.telegram.handler

import canoe.api._
import canoe.models.messages.TextMessage
import canoe.syntax._
import tbank.academy.domain.client.TgClient

object NotFoundHandler {
  def make[F[_]](client: TgClient[F], commandNames: List[String]): Handler[F] = new Handler[F] {
    override val scenario: Scenario[F, Unit] = for {
      message <- Scenario.expect(incorrectCommand)
      _       <- Scenario.eval(client.sendMessage(
        message.chat.id,
        "Не совсем понимаю, что имелось ввиду. Cписок доступных комманд /help"
      ))
    } yield ()

    def incorrectCommand: Expect[TextMessage] =
      textMessage.startingWith("/").when(message => !commandNames.contains(message.text))
  }
}
