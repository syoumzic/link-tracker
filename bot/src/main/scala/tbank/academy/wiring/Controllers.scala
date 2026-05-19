package tbank.academy.wiring

import cats.effect.Async
import tbank.academy.adapters.controller.http.BotController
import tbank.academy.domain.telegram.TgService

case class Controllers[F[_]](scrapperController: BotController[F])

object Controllers {
  def make[F[_]: Async](tgService: TgService[F]): Controllers[F] = Controllers(
    BotController.make[F](tgService)
  )
}
