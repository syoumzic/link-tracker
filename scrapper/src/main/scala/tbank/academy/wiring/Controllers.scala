package tbank.academy.wiring

import cats.effect.Async
import tbank.academy.adapter.controller.http.ScrapperController
import tofu.logging.Logging

case class Controllers[F[_]](botController: ScrapperController[F])

object Controllers {
  def make[F[_]: Async: Logging.Make](repositories: Repositories[F]): Controllers[F] = new Controllers(
    ScrapperController.make[F](repositories.chatRepository)
  )
}
