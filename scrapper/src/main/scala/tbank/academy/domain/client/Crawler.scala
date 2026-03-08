package tbank.academy.domain.client

import tbank.academy.domain.model.{Link, Site}

trait Crawler[F[_]] {
  val site: Site

  def requestUpdate(link: Link): F[Option[Link]]
}
