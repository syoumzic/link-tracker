package tbank.academy

import cats.data.ReaderT
import cats.effect.{IO, IOApp}
import tbank.academy.adapter.Server
import tbank.academy.config.AppConfig
import tbank.academy.domain.service.{GithubAnalyzer, StackoverflowAnalyzer}
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
    repositories <- Repositories.make[AppT]
    clients      <- Clients.make[AppT]
    services     <- Services.make[AppT](repositories)
    controllers  <- Controllers.make[AppT](services)
    _            <- Server.make[AppT](controllers.scrapperController)
    _            <- GithubAnalyzer.make[AppT](clients.githubClient, clients.botClient, repositories.linkRepository)
    _ <- StackoverflowAnalyzer.make[AppT](clients.stackoverflowClient, clients.botClient, repositories.linkRepository)
  } yield ()).useForever
}
