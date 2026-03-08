package tbank.academy.domain.model

import cats.MonadThrow
import cats.effect.Async
import cats.implicits._
import derevo.derive
import sttp.model.Uri
import tofu.logging.Loggable
import tofu.logging.derivation.loggable

import java.time.Instant
import scala.util.matching.Regex

@derive(loggable)
case class Link(
    chatIds: List[TgChat.Id],
    tags: List[Link.Tag],
    uri: Uri,
    apiUrl: Uri,
    site: Site,
    lastUpdate: Option[Instant] = None
)

object Link {
  trait Error extends DomainError

  case class LinkAlreadyExist(uri: String) extends Error
  case class LinkNotFound(uri: String)     extends Error
  case class UnexpectedLink(uri: String)   extends Error

  type Tag = String

  private val githubUrl: Regex        = """https://api.github\.com/repos/(\w+)/(\w+)""".r
  private val stackoverflowUrl: Regex = """https://api.stackexchange\.com/questions/(\d+)/.*""".r

  implicit val uriLoggable: Loggable[Uri] = Loggable.stringValue.contramap(_.pathToString)

  def apply(chatId: TgChat.Id, tags: List[Tag], url: Uri, apiUrl: Uri, site: Site): Link =
    Link(List(chatId), tags, url, apiUrl, site)

  def generate[F[_]: Async](chatId: TgChat.Id, tags: List[Tag], rawUrl: String)(implicit R: MonadThrow[F]): F[Link] =
    Uri.safeApply(rawUrl) match {
      case Right(url) => rawUrl match {
          case githubUrl(user, repo) =>
            Link(
              chatId = chatId,
              tags = tags,
              url = url,
              apiUrl = Uri(s"https://api.github.com/repos/$user/$repo"),
              site = Site.Github
            ).pure[F]

          case stackoverflowUrl(questionId) => Link(
              chatId = chatId,
              tags = tags,
              url = url,
              apiUrl = Uri(s"https://api.stackexchange.com/2.3/questions/$questionId?site=stackoverflow"),
              site = Site.StackOverflow
            ).pure[F]

          case _ => R.raiseError(UnexpectedLink(rawUrl))
        }

      case _ => R.raiseError(UnexpectedLink(rawUrl))
    }
}
