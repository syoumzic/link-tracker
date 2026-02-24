package tbank.academy.domain.service

import canoe.api._
import canoe.models.BotCommand
import canoe.models.messages.TextMessage
import canoe.syntax._
import cats.effect._
import tbank.academy.domain.model.{Message, User}
import tofu.logging.LoggingCompanion
import tofu.syntax.logging._
import fs2.Stream

trait BotAlg[F[_]] {
  def run: F[Unit]
}

object BotAlg extends LoggingCompanion[BotAlg] {
  def make[F[_]: Async: BotAlg.Log](token: String, userRepo: UserRepository[F]): Resource[F, BotAlg[F]] = {
    TelegramClient[F](token).map(implicit client => build[F](Bot.polling, userRepo))
  }

  def build[F[_]: Async: BotAlg.Log: TelegramClient](
      bot: Bot[F],
      userRepo: UserRepository[F],
  ): BotAlg[F] = new BotAlg[F] {
    val commands: List[BotCommand] = List(BotCommand("/start", "запуск бота"), BotCommand("/help", "помощь"))

    override def run: F[Unit] = {
      Stream.eval(info"бот начал свою работу")
        .flatMap(_ => bot.follow(start, help, notFound))
        .onFinalize(info"бот своё отработал")
        .compile
        .drain
    }

    def privateChat(commandName: String): Expect[Message] =
      command(commandName)
        .andThen(Function.unlift(m =>
          m.from.map(from =>
            Message(m.chat, User(from))
          )
        ))

    def incorrectCommand: Expect[TextMessage] =
      textMessage.when(message => !commands.map(_.command).contains(message.text))

    def start: Scenario[F, Unit] = for {
      message <- Scenario.expect(privateChat("start"))
      _       <- Scenario.eval(info"пользователь ${message.user} воспользовался командой /start")
      _       <- Scenario.eval(userRepo.save(message.user))
      _ <- Scenario.eval(message.chat.send("Добро пожаловать! Чтобы ознакомиться с тем, что я умею введите /help"))
    } yield ()

    def help: Scenario[F, Unit] = for {
      message <- Scenario.expect(privateChat("help"))
      _       <- Scenario.eval(info"пользователь ${message.user} воспользовался командой /help")
      _       <- Scenario.eval(message.chat.send(
        """Я бот для агрегации уведомлений. Я умею:
          |/start - вывести преветственное сообщение
          |/help - вывести help сообщение""".stripMargin
      ))
    } yield ()

    def notFound: Scenario[F, Unit] = for {
      message <- Scenario.expect(incorrectCommand)
      _ <- Scenario.eval(message.chat.send("Не совсем понимаю, что имелось ввиду. Cписок доступных комманд /help"))
    } yield ()
  }
}
