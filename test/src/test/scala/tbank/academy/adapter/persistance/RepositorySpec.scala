package tbank.academy.adapter.persistance

import cats.effect.IO
import com.dimafeng.testcontainers.PostgreSQLContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForEach
import org.flywaydb.core.Flyway
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tbank.academy.wiring.Repositories

trait RepositorySpec extends AnyFlatSpec with Matchers with TestContainerForEach {
  def mkRepository(containers: Containers): Repositories[IO]

  def migrate(containers: PostgreSQLContainer): Unit = {
    Flyway.configure()
      .dataSource(containers.jdbcUrl, containers.username, containers.password)
      .locations("filesystem:migrations")
      .load()
      .migrate()

    ()
  }
}
