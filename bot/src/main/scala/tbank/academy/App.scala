package tbank.academy

import cats.data.ReaderT
import cats.effect._
import tbank.academy.config.AppConfig
import tbank.academy.wiring.{Clients, Controllers, Services}
import tofu.logging.Logging

object App extends IOApp.Simple {
  implicit val loggingIO: Logging.Make[AppT] = Logging.Make.plain[AppT]

  type AppT[T] = ReaderT[IO, AppConfig, T]

  override def run: IO[Unit] =
    AppConfig
      .load[IO]
      .flatMap(application.run)
      .void

  def application: AppT[Nothing] = (for {
    clients <- Clients.make[AppT]
    controllers = Controllers.make[AppT](clients)
    _ <- Services.make[AppT](clients, controllers)
  } yield ()).useForever
}
