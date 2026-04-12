package tbank.academy.wiring

import cats.effect.implicits.effectResourceOps
import cats.effect.{Async, Resource}
import cats.implicits._
import doobie._
import tbank.academy.adapter.persistance.doobieDB
import tbank.academy.config.AppConfig
import tbank.academy.domain.repository._
import tofu.WithContext

case class Repositories[F[_]](
    chatRepository: ChatRepository[F],
    linkRepository: LinkRepository[F]
)

object Repositories {

  def make[F[_]](implicit context: WithContext[F, AppConfig], async: Async[F]): Resource[F, Repositories[F]] =
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
}
