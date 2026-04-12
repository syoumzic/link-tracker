package tbank.academy

import derevo.derive
import tofu.logging.derivation.loggable

import java.time.Instant

@derive(loggable)
case class Link(
    url: String,
    apiUrl: String,
    site: Site,
    processedCount: Long,
    tags: Set[String] = Set.empty,
    chatIds: Set[Long] = Set.empty,
    lastUpdate: Option[Instant] = None,
)

@derive(loggable)
sealed trait Site

case object Github extends Site

case object Stackoverflow extends Site

case class TgChat(id: Long, links: List[Link] = List.empty)
