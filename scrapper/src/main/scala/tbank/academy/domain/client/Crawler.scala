package tbank.academy.domain.client

import tbank.academy.Link

import scala.concurrent.duration.FiniteDuration

trait Crawler[F[_]] {
  def requestUpdate(link: Link, timeout: FiniteDuration): F[Option[Link]]
}
