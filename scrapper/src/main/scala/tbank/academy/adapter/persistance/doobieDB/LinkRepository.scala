package tbank.academy.adapter.persistance.doobieDB

import alleycats.std.all.alleycatsStdInstancesForSet
import cats.effect.Async
import cats.implicits._
import doobie.Update
import doobie.implicits._
import doobie.util.transactor.Transactor
import tbank.academy.Link
import tbank.academy.domain.{repository => domain}
import tbank.academy.adapter.persistance.doobieDB.Transformer._
import tbank.academy.domain.repository.LinkRepository.LinkNotFound

object LinkRepository {
  def make[F[_]](transactior: Transactor[F])(implicit async: Async[F]): domain.LinkRepository[F] =
    new domain.LinkRepository[F] {
      import doobie.implicits.javatimedrivernative._

      override def insertLink(chatId: Long, link: Link): F[Link] = (
        for {
          linkId <- sql"""
            INSERT INTO links (chatId, url, apiUrl, site, lastUpdate, processedCount)
            VALUES ($chatId, ${link.url}, ${link.apiUrl}, ${link.site}, ${link.lastUpdate}, ${link.processedCount})
            RETURNING id
          """
            .query[Long]
            .unique

          _ <- Update[(Long, String)](
            "INSERT INTO tags (linkId, name) VALUES (?, ?)"
          ).updateMany(link.tags.map((linkId, _)))
        } yield link
      ).transact(transactior)

      override def getLinks(chatId: Long): F[List[Link]] =
        sql"""
          SELECT l.url, l.apiUrl, l.site,
                 array_agg(DISTINCT t.name) FILTER (WHERE t.name IS NOT NULL),
                 array_agg(DISTINCT l.chatId),
                 l.lastUpdate, l.processedCount
          FROM links l
          LEFT JOIN tags t ON l.id = t.linkId
          WHERE l.chatId = $chatId
          GROUP BY l.url, l.apiUrl, l.site, l.lastUpdate, l.processedCount
        """.query[Link]
          .to[List]
          .transact(transactior)
          .ensure(LinkNotFound)(_.nonEmpty)

      override def getLinks(chatId: Long, tag: String): F[List[Link]] =
        sql"""
          SELECT l.url, l.apiUrl, l.site,
                 array_agg(t.name) as tags,
                 array_agg(l.chatId) as chatIds,
                 l.lastUpdate, l.processedCount
          FROM links l
          LEFT JOIN tags t ON l.id = t.linkId
          WHERE l.chatId = $chatId
            AND l.id IN (SELECT linkId FROM tags WHERE name = $tag)
          GROUP BY l.url, l.apiUrl, l.site, l.lastUpdate, l.processedCount
        """
          .query[Link]
          .to[List]
          .transact(transactior)
          .ensure(LinkNotFound)(_.nonEmpty)

      override def getLinks: F[List[Link]] = {
        sql"""
          SELECT l.url, l.apiUrl, l.site,
                 (SELECT array_agg(DISTINCT name) FROM tags WHERE linkId IN (SELECT id FROM links WHERE url = l.url)) as tags,
                 array_agg(DISTINCT l.chatId) as chatIds,
                 l.lastUpdate, l.processedCount
          FROM links l
          GROUP BY l.url, l.apiUrl, l.site, l.lastUpdate, l.processedCount
        """
          .query[Link]
          .to[List]
          .transact(transactior)
      }

      override def deleteLink(chatId: Long, url: String): F[Link] = (for {
        link <- sql"""
          SELECT l.url, l.apiUrl, l.site,
                 array_agg(t.name) FILTER (WHERE t.name IS NOT NULL),
                 array_agg(DISTINCT l.chatId),
                 l.lastUpdate, l.processedCount
          FROM links l
          LEFT JOIN tags t ON l.id = t.linkId
          WHERE l.chatId = $chatId AND l.url = $url
          GROUP BY l.url, l.apiUrl, l.site, l.lastUpdate, l.processedCount
        """.query[Link].unique

        _ <- sql"DELETE FROM links WHERE chatId = $chatId AND url = $url".update.run

      } yield link).transact(transactior)

      override def updateCount(url: String, count: Long): F[Unit] =
        sql"""
         UPDATE links
         SET processedCount = $count
         WHERE url = $url
        """
          .update
          .run
          .transact(transactior)
          .void
    }
}
