package tbank.academy.domain.telegram.command

import canoe.api.Scenario
import canoe.syntax.{Expect, text}
import cats.effect.Async
import cats.implicits._
import tbank.academy.adapters.client.http.ScrapperClient.{LinkAlreadyExist, UnexpectedLink}
import tbank.academy.domain.client.{ScrapperClient, TgClient}
import tbank.academy.domain.telegram.command.Command.privateChat

import scala.util.matching.Regex

trait TrackCommand[F[_]] extends Command[F]

object TrackCommand {
  def make[F[_]: Async](client: TgClient[F], scrapperClient: ScrapperClient[F]): TrackCommand[F] = {
    new TrackCommand[F] {
      override val name: String        = "/track"
      override val description: String = "Подписаться на уведомления"

      private val tagsRegex: Regex = """((?: *\w+ *,)* *\w+ *)""".r

      def scenario: Scenario[F, Unit] = for {
        chat <- Scenario.expect(privateChat(name))
        _    <- Scenario.eval(client.sendMessage(
          chat,
          "Введите ссылку (поддерживается отслеживание github repository, stackoverflow question)"
        ))
        url  <- Scenario.expect(text)
        _    <- Scenario.eval(client.sendMessage(chat, "Можете добавить ссылке теги"))
        tags <- Scenario.expect(tagsOption)
        _    <- Scenario.eval(
          scrapperClient
            .linksPost(chat.id, url, tags.getOrElse(Set.empty), Set.empty)
            .flatMap { _ =>
              client.sendMessage(chat, "Ссылка успешно добавлена")
            }
            .recoverWith {
              case LinkAlreadyExist(message) => client.sendMessage(chat, message)
              case UnexpectedLink(message)   => client.sendMessage(chat, message)
            }
        )
      } yield ()

      def tagsOption: Expect[Option[Set[String]]] = text.andThen {
        case tagsRegex(tags) => Some(tags.split(",").map(_.trim).toSet)
        case ""              => None
      }
    }
  }
}
