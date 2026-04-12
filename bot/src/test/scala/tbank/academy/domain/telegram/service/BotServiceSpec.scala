package tbank.academy.domain.telegram.service

import canoe.api._
import canoe.models.messages._
import canoe.models.{PrivateChat, User => CanoeUser}
import cats.effect._
import cats.effect.unsafe.implicits.global
import cats.implicits._
import fs2.Stream
import org.scalamock.scalatest.MockFactory
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tbank.academy.domain.client.{ScrapperClient, TgClient}
import tbank.academy.domain.telegram.command.{HelpCommand, ListCommand, StartCommand, TrackCommand, UntrackCommand}
import tbank.academy.http.{LinkResponse, ListLinksResponse}
import tofu.logging.Logging

import scala.annotation.nowarn
import scala.collection.mutable

// scalafix:off Disable.collection.mutable
@nowarn("msg=unused value")
class BotServiceSpec extends AnyFlatSpec with Matchers with MockFactory {
  implicit val loggingMake: Logging.Make[IO] = (_: String) => Logging.empty[IO]

  val unknownMessage: String = "Не совсем понимаю, что имелось ввиду. Cписок доступных комманд /help"

  val tgClient: TgClient[IO]             = mock[TgClient[IO]]
  val scrapperClient: ScrapperClient[IO] = mock[ScrapperClient[IO]]

  def updates(messages: List[String]): Stream[IO, TelegramMessage] =
    Stream
      .emits(messages)
      .map { message =>
        TextMessage(
          messageId = 0,
          chat = PrivateChat(id = 0, None, None, None),
          date = 0,
          text = message,
          from = Some(CanoeUser(id = 0, isBot = false, "Anton", None, None, None, None, None, None))
        )
      }

  def commandTest(
      input: List[String],
      expected: List[String],
      inputLinks: List[String] = List.empty,
      updateLinkExpected: List[String] = List.empty,
      deleteLinkExpected: List[String] = List.empty,
      scenario: Scenario[IO, Unit]
  ): Assertion = {
    val outputQueue: mutable.Queue[String] = mutable.Queue.empty

    (tgClient.sendMessage _)
      .expects(*, *)
      .onCall { (_, message) => outputQueue.enqueue(message).pure[IO].void }
      .anyNumberOfTimes()

    (scrapperClient.linksGet _)
      .expects(*, *)
      .onCall { (_, tag) =>
        ListLinksResponse(
          inputLinks.map(LinkResponse(0, _, tag.map(Set(_)).getOrElse(Set.empty), Set.empty)),
          inputLinks.size
        ).pure[IO]
      }
      .anyNumberOfTimes()

    val updateQueue: mutable.Queue[String] = mutable.Queue.empty

    (scrapperClient.linksPost _)
      .expects(*, *, *, *)
      .onCall { (_, url, tags, filters) =>
        updateQueue.enqueue(url).pure[IO] >> LinkResponse(0, url, tags, filters).pure[IO]
      }
      .anyNumberOfTimes()

    val deleteQueue: mutable.Queue[String] = mutable.Queue.empty

    (scrapperClient.linksDelete _)
      .expects(*, *)
      .onCall { (_, url) =>
        deleteQueue.enqueue(url).pure[IO] >> LinkResponse(0, url, Set.empty, Set.empty).pure[IO]
      }
      .anyNumberOfTimes()

    updates(input)
      .through(scenario.pipe)
      .compile
      .drain
      .unsafeRunSync()

    assert(
      outputQueue.toList == expected &&
        updateQueue.toList == updateLinkExpected &&
        deleteQueue.toList == deleteLinkExpected
    )
  }

  "start command" should "start" in commandTest(
    scenario = StartCommand.make(tgClient).scenario,
    input = List("/start"),
    expected = List("Добро пожаловать! Чтобы ознакомиться с тем, что я умею введите /help")
  )

  "help command" should "help" in commandTest(
    scenario = HelpCommand.make(tgClient).scenario,
    input = List("/help"),
    expected = List("""Я бот для агрегации уведомлений. Я умею:
                      |/start - вывести преветственное сообщение
                      |/help - вывести help сообщение""".stripMargin)
  )

  "track command" should "add link" in commandTest(
    scenario = TrackCommand.make(tgClient, scrapperClient).scenario,
    input = List(
      "/track",
      "https://api.github.com/repos/typelevel/cats-effect",
      "",
    ),
    expected = List(
      "Введите ссылку (поддерживается отслеживание github repository, stackoverflow question)",
      "Можете добавить ссылке теги",
      "Ссылка успешно добавлена"
    ),
    updateLinkExpected = List("https://api.github.com/repos/typelevel/cats-effect")
  )
  "unctrack command" should "untrack link" in commandTest(
    scenario = UntrackCommand.make(tgClient, scrapperClient).scenario,
    input = List(
      "/untrack",
      "https://api.github.com/repos/typelevel/cats-effect",
      ""
    ),
    expected = List(
      "Введите ссылку которую хотите перестать отслеживать",
      "Ссылка успешно удалена"
    ),
    deleteLinkExpected =
      List("https://api.github.com/repos/typelevel/cats-effect")
  )

  "list command" should "list" in commandTest(
    scenario = ListCommand.make(tgClient, scrapperClient).scenario,
    input = List(
      "/list",
      ""
    ),
    expected = List(
      "Для уточнения можете ввести интересующий вас тег",
      "Ссылки: https://api.github.com/repos/typelevel/cats-effect"
    ),
    inputLinks = List("https://api.github.com/repos/typelevel/cats-effect")
  )
}
// scalafix:on Disable.collection.mutable
