package tbank.academy.domain.service

import canoe.api._
import canoe.methods.Method
import canoe.methods.messages.SendMessage
import canoe.models.messages._
import canoe.models.{MessageReceived, PrivateChat, Update, User => CanoeUser}
import cats.effect._
import cats.effect.unsafe.implicits.global
import fs2.Stream
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tbank.academy.domain.model._
import tofu.logging.Logging

import scala.collection.immutable.Queue
import scala.concurrent.duration.DurationDouble

class BotAlgSpec extends AnyFlatSpec with Matchers {
  type ChatId  = Int
  type Message = String

  implicit val loggingMake: Logging.Make[IO] = Logging.Make.plain[IO]
  implicit val loggingIO: Logging[IO]        = loggingMake.byName("testing")

  val user: CanoeUser        = CanoeUser(1, isBot = false, "mock", None, None, None, None, None, None)
  val unknownMessage: String = "Не совсем понимаю, что имелось ввиду. Cписок доступных комманд /help"
  val token: String          = "<token>"

  val userRepo: UserRepository[IO] = (_: User) => IO.unit

  def textMessage(text: Message): TextMessage =
    TextMessage(1, PrivateChat(1, None, None, None), -1, text, from = Some(user))

  def messageReceived(text: Message): MessageReceived =
    MessageReceived(1, textMessage(text))

  def updates(messages: List[Message]): Stream[IO, Update] =
    Stream
      .emits(messages)
      .map(messageReceived)
      .metered[IO](0.2.seconds)

  def commandTest(input: List[Message], expected: List[Message]): Assertion = (for {
    ref <- Ref.of[IO, Queue[String]](Queue.empty[String])
    bot = {
      implicit val client: TelegramClient[IO] = new TelegramClient[IO] {
        override def execute[Req, Res](request: Req)(implicit M: Method[Req, Res]): IO[Res] =
          ref.update(_.enqueue(request.asInstanceOf[SendMessage].text)) >>
            IO(textMessage("callback").asInstanceOf[Res])
      }
      BotAlg.build(Bot.fromStream(updates(input)), userRepo)
    }
    _      <- bot.run
    output <- ref.get
  } yield output.toList shouldBe expected).unsafeRunSync()

  "bot" should "start" in commandTest(
    input = List("/start"),
    expected = List("Добро пожаловать! Чтобы ознакомиться с тем, что я умею введите /help")
  )

  it should "help" in commandTest(
    input = List("/help"),
    expected = List("""Я бот для агрегации уведомлений. Я умею:
                      |/start - вывести преветственное сообщение
                      |/help - вывести help сообщение""".stripMargin)
  )

  it should "interrupt incorrect commands" in commandTest(
    input = List(
      "скажи 300",
      "ну скажи 300!",
      "да ну тебя(",
      "/scathi300"
    ),
    expected = List(
      unknownMessage,
      unknownMessage,
      unknownMessage,
      unknownMessage
    )
  )
}
