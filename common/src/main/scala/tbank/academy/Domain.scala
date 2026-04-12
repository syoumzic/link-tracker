package tbank.academy

import derevo.derive
import tofu.logging.derivation.loggable

import java.time.Instant

@derive(loggable)
case class Link(
    url: String,
    apiUrl: String,
    site: Site,
    tags: Set[String],
    chatIds: Set[Long],
    lastUpdate: Option[Instant] = None
)

@derive(loggable)
sealed trait Site

case object Github extends Site

case object Stackoverflow extends Site

@derive(loggable)
sealed trait LinkInfo

@derive(loggable)
case class GithubInfo(lastUpdate: Instant) extends LinkInfo

@derive(loggable)
case class StackoverflowInfo(lastUpdate: Instant) extends LinkInfo

case class TgChat(id: Long, links: List[Link] = List.empty)
