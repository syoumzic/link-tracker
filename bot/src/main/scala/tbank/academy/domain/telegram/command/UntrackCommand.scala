package tbank.academy.domain.telegram.command

import canoe.api.Scenario
import canoe.syntax.text
import cats.effect.Async
import cats.implicits._
import tbank.academy.domain.client.{ScrapperClient, TgClient}
import tbank.academy.domain.model.Link.LinkNotFound
import tbank.academy.domain.telegram.command.Command.privateChat

trait UntrackCommand[F[_]] extends Command[F]

object UntrackCommand {
  def make[F[_]: Async](client: TgClient[F], scrapperClient: ScrapperClient[F]): UntrackCommand[F] = {
    new UntrackCommand[F] {
      override val name: String        = "/untrack"
      override val description: String = "Перестать отслеживать ссылку"

      def scenario: Scenario[F, Unit] = for {
        chat <- Scenario.expect(privateChat(name))
        _    <- Scenario.eval(client.sendMessage(
          chat,
          "Введите ссылку которую хотите перестать отслеживать"
        ))
        url <- Scenario.expect(text)
        _   <- Scenario.eval(
          scrapperClient
            .linksDelete(chat.id, url)
            .flatMap { _ =>
              client.sendMessage(chat, "Ссылка успешно удалена")
            }
            .recoverWith {
              case LinkNotFound(uri) => client.sendMessage(chat, s"Похоже ссылка $uri уже не отслеживалась")
            }
        )
      } yield ()
    }
  }
}
