package tbank.academy.adapter.persistance.slickDB

import cats.effect.Async
import cats.implicits._
import slick.dbio.DBIO
import slick.jdbc.JdbcBackend.Database
import tbank.academy.{Link, Site}
import tbank.academy.domain.{repository => domain}
import tbank.academy.adapter.persistance.slickDB.Domain._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits._
import io.scalaland.chimney.syntax._
import tbank.academy.adapter.persistance.slickDB.Transfromer._
import tbank.academy.domain.repository.LinkRepository.LinkNotFound

object LinkRepository {
  def make[F[_]](database: Database)(implicit async: Async[F]): domain.LinkRepository[F] =
    new domain.LinkRepository[F] {
      private def runQuery[T](action: DBIO[T]): F[T] =
        async.fromFuture(async.delay(database.run(action)))

      def insertLink(chatId: Long, link: Link): F[Link] = {
        val action = for {
          linkId <- (links returning links.map(_.id)) += LinkRow(
            id = 0L,
            chatId = chatId,
            url = link.url,
            apiUrl = link.apiUrl,
            site = link.site.toString.toLowerCase,
            lastUpdate = link.lastUpdate
          )

          _ <- tags ++= link.tags.map(tag => (linkId, tag))
        } yield link

        runQuery(action.transactionally)
      }

      def getLinks(chatId: Long): F[List[Link]] = {
        val action = (for {
          (l, t) <- links
            .filter(_.chatId === chatId)
            .joinLeft(tags).on(_.id === _.linkId)
        } yield (l, t.map(_.name)))
          .result
          .map(mkLinksOpt)

        runQuery(action).ensure(LinkNotFound)(_.nonEmpty)
      }

      def getLinks(chatId: Long, tag: String): F[List[Link]] = {
        val action = (for {
          linkId <- tags.filter(_.name === tag).map(_.linkId)
          l      <- links.filter(l => l.chatId === chatId && l.id === linkId)
          t      <- tags.filter(_.linkId === l.id)
        } yield (l, t.name))
          .result
          .map(mkLinks)

        runQuery(action).ensure(LinkNotFound)(_.nonEmpty)
      }

      def getLinks: F[List[Link]] = {
        val action = (for {
          l <- links
          t <- tags.filter(_.linkId === l.id)
        } yield (l, t.name))
          .result
          .map(mkLinks)

        runQuery(action)
      }

      def updateLinks(linksList: List[Link]): F[Unit] = {
        val actions = linksList.map { link =>
          links
            .filter(_.url === link.url)
            .map(_.lastUpdate)
            .update(link.lastUpdate)
        }

        runQuery(DBIO.sequence(actions).transactionally).void
      }

      def deleteLink(chatId: Long, url: String): F[Link] = {
        val action = for {
          // Сначала получаем информацию о ссылке
          linksData <- (for {
            (l, t) <- links
              .filter(l => l.chatId === chatId && l.url === url)
              .joinLeft(tags).on(_.id === _.linkId)
          } yield (l, t.map(_.name)))
            .result
            .map(results =>
              results.map(result => {
                val linkRow = result._1
                Link(
                  url = linkRow.url,
                  apiUrl = linkRow.apiUrl,
                  site = linkRow.site.transformInto[Site],
                  tags = results.flatMap(_._2).toSet,
                  chatIds = Set(linkRow.chatId),
                  lastUpdate = linkRow.lastUpdate
                )
              })
            )

          // Затем удаляем ссылку (теги удалятся каскадно, если настроен FK)
          linkId <- links
            .filter(l => l.chatId === chatId && l.url === url)
            .map(_.id)
            .result
            .headOption
            .flatMap {
              case Some(id) => DBIO.successful(id)
              case None     => DBIO.failed(LinkNotFound)
            }

          _ <- tags.filter(_.linkId === linkId).delete
          _ <- links.filter(l => l.chatId === chatId && l.url === url).delete
        } yield linksData

        runQuery(action.transactionally).flatMap(_.headOption.liftTo[F](LinkNotFound))
      }

      private def mkLinks(results: Seq[(LinkRow, String)]): List[Link] =
        mkLinksOpt(results.map { case (linkRow, grouped) => (linkRow, grouped.some) })

      private def mkLinksOpt(results: Seq[(LinkRow, Option[String])]): List[Link] =
        results
          .groupBy(_._1)
          .map { case (linkRow, grouped) =>
            Link(
              url = linkRow.url,
              apiUrl = linkRow.apiUrl,
              site = linkRow.site.transformInto[Site],
              tags = grouped.flatMap(_._2).toSet,
              chatIds = Set(linkRow.chatId),
              lastUpdate = linkRow.lastUpdate
            )
          }
          .toList
    }
}
