package tbank.academy.domain.model

import derevo.derive
import io.estatico.newtype.macros.newtype
import org.http4s.Uri
import tbank.academy.domain.model.User.UserId
import tofu.logging.Loggable
import tofu.logging.derivation.loggable
import canoe.models.{User => CanoeUser}

@derive(loggable)
case class User(
    id: UserId,
    firstName: String,
    lastName: Option[String],
    username: Option[String],
    languageCode: Option[String],
    references: Option[List[Uri]] = None
)

object User {
  @derive(loggable)
  @newtype case class UserId(id: Long)

  implicit val loggableUri: Loggable[Uri] = Loggable[String].contramap(_.renderString)

  def apply(rawUser: CanoeUser): User = User(
    id = UserId(rawUser.id),
    firstName = rawUser.firstName,
    lastName = rawUser.lastName,
    username = rawUser.username,
    languageCode = rawUser.languageCode
  )
}
