package tbank.academy.wiring

import cats.effect.implicits.effectResourceOps
import cats.effect.{Async, Resource}
import cats.implicits._
import doobie._
import slick.jdbc.JdbcBackend.Database
import tbank.academy.DomainError
import tbank.academy.adapter.persistance.{doobieDB, slickDB}
import tbank.academy.config.AppConfig
import tbank.academy.domain.repository._
import tofu.WithContext

case class Repositories[F[_]](
    chatRepository: ChatRepository[F],
    linkRepository: LinkRepository[F]
)

object Repositories {
  sealed trait Error extends DomainError

  case object InvalidOrmType extends Error

  def make[F[_]](implicit context: WithContext[F, AppConfig], async: Async[F]): Resource[F, Repositories[F]] = {
    context.ask(_.`access-type`).toResource.flatMap {
      case "ORM" => slick
      case "SQL" => doobie
      case _     => async.raiseError[Repositories[F]](InvalidOrmType).toResource
    }
  }

  def doobie[F[_]: Async](implicit context: WithContext[F, AppConfig]): Resource[F, Repositories[F]] =
    context.ask(_.doobie).map(cfg => {

      val transactor = Transactor.fromDriverManager[F](
        driver = cfg.driver,
        url = cfg.url,
        user = cfg.user,
        password = cfg.password,
        logHandler = None
      )

      Repositories(
        doobieDB.ChatRepository.make[F](transactor),
        doobieDB.LinkRepository.make[F](transactor)
      )
    }).toResource

  def slick[F[_]: Async]: Resource[F, Repositories[F]] = {
    val db = Database.forConfig("slick")

    Resource.pure(
      Repositories(
        slickDB.ChatRepository.make[F](db),
        slickDB.LinkRepository.make[F](db)
      )
    )
  }
}
