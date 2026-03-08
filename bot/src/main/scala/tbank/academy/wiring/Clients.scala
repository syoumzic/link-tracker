package tbank.academy.wiring

import cats.effect.Async
import cats.effect.kernel.Resource
import tbank.academy.adapters.telegram
import tbank.academy.domain.client.BotClient
import tbank.academy.domain.model.Config

case class Clients[F[_]](botClient: BotClient[F])

object Clients {
  def make[F[_]: Async](config: Config): Resource[F, Clients[F]] =
    telegram.BotClient.make[F](config.bot.token).map(Clients.apply)
}
