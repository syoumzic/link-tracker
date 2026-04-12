package tbank.academy.wiring

import cats.effect.{Async, Resource}
import tbank.academy.adapter.controller.http.ScrapperController
import tofu.logging.Logging

case class Controllers[F[_]](scrapperController: ScrapperController[F])

object Controllers {
  def make[F[_]: Async: Logging.Make](services: Services[F]): Resource[F, Controllers[F]] = Resource.pure(
    new Controllers(
      ScrapperController.make[F](services.chatService, services.linkService)
    )
  )
}
