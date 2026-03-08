package tbank.academy.wiring

import cats.effect.Async
import tbank.academy.adapters.controller.http.BotController

case class Controllers[F[_]](scrapperController: BotController[F])

object Controllers {
  def make[F[_]: Async](clients: Clients[F]): Controllers[F] = Controllers(
    BotController.make[F](clients.tgClient)
  )
}
