package tbank.academy.adapter.persistance

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import slick.jdbc.JdbcBackend.Database
import tbank.academy.adapter.{persistance => domain}
import tbank.academy.wiring.Repositories
import tofu.logging.Logging

class SlickSpec extends domain.LinkRepositorySpec with domain.ChatRepositorySpec {
  override val containerDef: PostgreSQLContainer.Def = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:15-alpine")
  )

  override def afterContainersStart(containers: PostgreSQLContainer): Unit = migrate(containers)

  implicit val loggingMake: Logging.Make[IO] = (_: String) => Logging.empty

  override def mkRepository(containers: PostgreSQLContainer): Repositories[IO] = {
    val db = Database.forURL(
      driver = containers.driverClassName,
      url = containers.jdbcUrl,
      user = containers.username,
      password = containers.password
    )

    Repositories(slickDB.ChatRepository.make[IO](db), slickDB.LinkRepository.make[IO](db))
  }
}
