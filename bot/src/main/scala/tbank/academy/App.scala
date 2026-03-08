package tbank.academy

import cats.effect._
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import tbank.academy.domain.api
import tbank.academy.domain.model.Config
import tbank.academy.wiring.Clients
import tofu.logging.Logging

object App extends IOApp.Simple {
  implicit val loggingIO: Logging.Make[IO] = Logging.Make.plain[IO]

  override def run: IO[Unit] = (for {
    config  <- Resource.pure(ConfigSource.default.loadOrThrow[Config])
    clients <- Clients.make[IO](config)
    bot = api.Bot.make[IO](clients.botClient)
    _ <- bot.run.toResource
  } yield ()).use_
}
