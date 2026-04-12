package tbank.academy.adapter.persistance

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import org.testcontainers.utility.DockerImageName
import tbank.academy.adapter.{persistance => domain}
import tbank.academy.wiring.Repositories
import tofu.logging.Logging

class DoobieSpec extends domain.ChatRepositorySpec with domain.LinkRepositorySpec {
  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:15-alpine")
  )

  implicit val loggingMake: Logging.Make[IO] = (_: String) => Logging.empty

  override def afterContainersStart(containers: PostgreSQLContainer): Unit = migrate(containers)

  override def mkRepository(containers: PostgreSQLContainer): Repositories[IO] = {
    val transactor: Transactor[IO] = Transactor.fromDriverManager(
      driver = containers.driverClassName,
      url = containers.jdbcUrl,
      user = containers.username,
      password = containers.password,
      logHandler = None
    )

    Repositories(doobieDB.ChatRepository.make[IO](transactor), doobieDB.LinkRepository.make[IO](transactor))
  }
}
