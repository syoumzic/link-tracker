package tbank.academy.domain.telegram.service

import canoe.api._
import canoe.models.messages._
import canoe.models.{BotCommand, Chat, MessageReceived, PrivateChat, Update, User => CanoeUser}
import cats.effect._
import cats.effect.unsafe.implicits.global
import cats.implicits._
import fs2.Stream
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tbank.academy.domain.client.{ScrapperClient, TgClient}
import tbank.academy.domain.telegram.TgService
import tbank.academy.http.{LinkResponse, ListLinksResponse}
import tofu.logging.Logging

import scala.collection.immutable.Queue
import scala.concurrent.duration.DurationDouble

class BotServiceSpec extends AnyFlatSpec with Matchers {
  implicit val loggingIO: Logging[IO] = Logging.empty[IO]

  implicit val loggingMake: Logging.Make[IO] = (_: String) => loggingIO

  val unknownMessage: String = "Не совсем понимаю, что имелось ввиду. Cписок доступных комманд /help"

  val userId: Long = 0

  val chatId: Long = 0

  val date: Int = 0

  val name: String = "Anton"

  val user: CanoeUser = CanoeUser(userId, isBot = false, name, None, None, None, None, None, None)

  val privateChat: PrivateChat = PrivateChat(chatId, None, None, None)

  def textMessageMake(text: String, textId: Int): TextMessage =
    TextMessage(textId, privateChat, date, text, from = Some(user))

  def messageReceivedMake(text: String, index: Long): MessageReceived =
    MessageReceived(index, textMessageMake(text, index.toInt))

  def updatesMake(messages: List[String]): Stream[IO, Update] =
    Stream
      .emits(messages)
      .zipWithIndex
      .map { case (message, index) => messageReceivedMake(message, index) }
      .metered[IO](0.2.second)

  def tgClientMake(
      input: List[String],
      outputRef: Ref[IO, Queue[String]]
  ): TgClient[IO] = new TgClient[IO] {
    override def setMyCommands(commands: List[BotCommand]): IO[Unit] = IO.unit

    override def sendMessage(chat: Chat, message: String): IO[Unit] = outputRef.update(_.enqueue(message))

    override def pooling: Bot[IO] = Bot.fromStream(updatesMake(input))

    override def updateLink(chat: Long, uri: String): IO[Unit] = outputRef.update(_.enqueue(uri))
  }

  def scrapperClientMake(
      getLinks: List[String],
      updateRef: Ref[IO, Queue[String]],
      deleteRef: Ref[IO, Queue[String]]
  ): ScrapperClient[IO] = new ScrapperClient[IO] {

    override def tgChatPost(chatId: Long): IO[Unit] = IO.unit

    override def tgChatDelete(chatId: Long): IO[Unit] = IO.unit

    override def linksGet(chatId: Long, tag: Option[String]): IO[ListLinksResponse] =
      ListLinksResponse(
        getLinks.map(LinkResponse(0, _, tag.map(Set(_)).getOrElse(Set.empty), Set.empty)),
        getLinks.size
      ).pure[IO]

    override def linksPost(chatId: Long, url: String, tags: Set[String], filters: Set[String]): IO[LinkResponse] =
      updateRef.update(_.enqueue(url)) >> LinkResponse(0, url, tags, filters).pure[IO]

    override def linksDelete(chatId: Long, url: String): IO[LinkResponse] =
      deleteRef.update(_.enqueue(url)) >> LinkResponse(0, url, Set.empty, Set.empty).pure[IO]
  }

  def commandTest(
      input: List[String],
      expected: List[String],
      inputLinks: List[String] = List.empty,
      updateLinkExpected: List[String] = List.empty,
      deleteLinkExpected: List[String] = List.empty,
  ): Assertion = (for {
    outputRef <- Ref.of[IO, Queue[String]](Queue.empty[String])
    updateRef <- Ref.of[IO, Queue[String]](Queue.empty[String])
    deleteRef <- Ref.of[IO, Queue[String]](Queue.empty[String])

    apiClient      = tgClientMake(input, outputRef)
    scrapperClient = scrapperClientMake(inputLinks, updateRef, deleteRef)
    bot            = TgService.make[IO](apiClient, scrapperClient)
    _          <- bot.run
    output     <- outputRef.get
    updateLink <- updateRef.get
    deleteLink <- deleteRef.get
  } yield assert(
    output.toList == expected && updateLink.toList == updateLinkExpected && deleteLink.toList == deleteLinkExpected
  )).unsafeRunSync()

  "start command" should "start" in commandTest(
    input = List("/start"),
    expected = List("Добро пожаловать! Чтобы ознакомиться с тем, что я умею введите /help")
  )

  "help command" should "help" in commandTest(
    input = List("/help"),
    expected = List("""Я бот для агрегации уведомлений. Я умею:
                      |/start - вывести преветственное сообщение
                      |/help - вывести help сообщение""".stripMargin)
  )

  "incorrect command" should "interrupt incorrect commands" in commandTest(
    input = List(
      "/скажи-300",
      "/ну-скажи-300!",
      "/да-ну-тебя(",
      "/scathi300"
    ),
    expected = List(
      unknownMessage,
      unknownMessage,
      unknownMessage,
      unknownMessage
    )
  )

  "track command" should "add link" in commandTest(
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
    updateLinkExpected =
      List("https://api.github.com/repos/typelevel/cats-effect")
  )

  "unctrack command" should "untrack link" in commandTest(
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
