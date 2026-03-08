package tbank.academy.domain.telegram.command

import canoe.api.Scenario
import canoe.syntax.{Expect, text}
import cats.effect.Async
import cats.implicits._
import tbank.academy.adapters.client.http.ScrapperClient.ChatNotFound
import tbank.academy.domain.client.{ScrapperClient, TgClient}
import tbank.academy.domain.model.Link
import tbank.academy.domain.model.http.ListLinksResponse
import tbank.academy.domain.telegram.command.Command.privateChat

import scala.util.matching.Regex

trait ListCommand[F[_]] extends Command[F]

object ListCommand {
  def make[F[_]: Async](client: TgClient[F], scrapperClient: ScrapperClient[F]): ListCommand[F] = {
    new ListCommand[F] {
      override val name: String        = "/list"
      override val description: String = "Вывести ссылки"

      private val tagRegex: Regex = """ *(\w+) *""".r

      def scenario: Scenario[F, Unit] = for {
        chat <- Scenario.expect(privateChat(name))
        _    <- Scenario.eval(client.sendMessage(chat, "Для уточнения можете ввести интересующий вас тег"))
        tag  <- Scenario.expect(tagOption)
        _    <- Scenario.eval(
          scrapperClient
            .linksGet(chat.id, tag)
            .flatMap {
              case ListLinksResponse(_, size) if size == 0 => client.sendMessage(chat, "Нет ссылок")
              case ListLinksResponse(links, _)             =>
                client.sendMessage(chat, s"Ссылки: ${links.map(_.uri).mkString("\n")}")
            }
            .recoverWith {
              case _: ChatNotFound => client.sendMessage(chat, "Чат не зарегистирован, введите /start")
            }
        )
      } yield ()

      def tagOption: Expect[Option[Link.Tag]] = text.andThen {
        case tagRegex(tag) => Some(tag)
        case ""            => None
      }
    }
  }
}
