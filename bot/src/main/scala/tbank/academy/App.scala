package tbank.academy

import cats.effect._
import pureconfig.ConfigSource
import pureconfig.generic.auto._
import tbank.academy.domain.model.Config
import tbank.academy.domain.service.{BotAlg, UserRepository}
import tofu.logging.Logging

object App extends IOApp.Simple {
  implicit val loggingIO: Logging.Make[IO] = Logging.Make.plain[IO]

  override def run: IO[Unit] = (for {
    config   <- Resource.pure(ConfigSource.default.loadOrThrow[Config])
    userRepo <- UserRepository.inMemory[IO].toResource
    botAlg   <- BotAlg.make[IO](config.bot.token, userRepo)
    _        <- botAlg.run.toResource
  } yield ()).use_
}
