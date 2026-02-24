package tbank.academy.domain.service

import cats.effect._
import cats.implicits._
import derevo.derive
import tbank.academy.domain.model.User
import tbank.academy.domain.model.User.UserId
import tofu.logging._
import tofu.logging.derivation._
import tofu.higherKind.derived.representableK

@derive(representableK, loggingMid)
trait UserRepository[F[_]] {
  def save(user: User): F[Unit]
}

object UserRepository extends LoggingCompanion[UserRepository] {
  def inMemory[F[_]: Concurrent: Logging.Make]: F[UserRepository[F]] =
    Ref.of(Map.empty[UserId, User]).map(repo => new InMemoryUserRepository[F](repo).attachLogs)

  final private class InMemoryUserRepository[F[_]](repo: Ref[F, Map[UserId, User]]) extends UserRepository[F] {
    override def save(user: User): F[Unit] = repo.update(_.updated(user.id, user))
  }
}
