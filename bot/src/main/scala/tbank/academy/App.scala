package tbank.academy

import cats.data.ReaderT
import cats.effect._
import tbank.academy.adapters.Server
import tbank.academy.config.AppConfig
import tbank.academy.domain.telegram.TgService
import tbank.academy.wiring.{Clients, Controllers}
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
    clients   <- Clients.make[AppT]
    tgService <- TgService.pooling[AppT](clients.tgClient, clients.scrapperClient)
    controllers = Controllers.make[AppT](tgService)
    _ <- Server.make[AppT](controllers.scrapperController)
  } yield ()).useForever
}
