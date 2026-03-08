package tbank.academy.domain.client

import tbank.academy.domain.model.Link

trait BotClient[F[_]] {
  def updateLink(link: Link): F[Unit]
}
