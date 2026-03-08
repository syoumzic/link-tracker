package tbank.academy

import cats.data.ReaderT
import cats.effect.implicits.effectResourceOps
import cats.effect.{IO, IOApp}
import tbank.academy.config.AppConfig
import tbank.academy.wiring.{Clients, Controllers, Repositories, Services}
import tofu.logging.Logging

object App extends IOApp.Simple {
  implicit val loggingIO: Logging.Make[AppT] = Logging.Make.plain[AppT]

  type AppT[T] = ReaderT[IO, AppConfig, T]

  override def run: IO[Unit] =
    AppConfig
      .load[IO]
      .flatMap(application.run)

  private def application: AppT[Nothing] = (for {
    repositories <- Repositories.make[AppT].toResource
    clients      <- Clients.make[AppT]
    controllers = Controllers.make[AppT](repositories)
    _ <- Services.make[AppT](repositories, clients, controllers)
  } yield ()).useForever
}
