package tbank.academy.adapter.persistance.doobieDB

import doobie.{Get, Put, Read}
import tbank.academy.{Github, Link, Site, Stackoverflow}
import doobie.postgres.implicits._
import java.time.Instant

object Transformer {
  implicit val sitePut: Put[Site] =
    Put[String].contramap(_.toString.toLowerCase)

  implicit val siteGet: Get[Site] =
    Get[String].temap {
      case "github"        => Right(Github)
      case "stackoverflow" => Right(Stackoverflow)
      case other           => Left(s"Unknown site in database: $other")
    }

  implicit val linkRead: Read[Link] =
    Read[(String, String, Site, Option[List[String]], Option[List[Long]], Option[Instant], Long)].map {
      case (url, apiUrl, site, tags, chatIds, lastUpdate, processedCount) =>
        Link(
          url = url,
          apiUrl = apiUrl,
          site = site,
          tags = tags.map(_.toSet).getOrElse(Set.empty[String]),
          chatIds = chatIds.map(_.toSet).getOrElse(Set.empty[Long]),
          lastUpdate = lastUpdate,
          processedCount = processedCount
        )
    }
}
