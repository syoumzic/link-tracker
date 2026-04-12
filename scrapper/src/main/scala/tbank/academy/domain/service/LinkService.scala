package tbank.academy.domain.service

import cats.effect.Async
import cats.implicits._
import tbank.academy.domain.repository.LinkRepository
import tbank.academy.{DomainError, Github, Link, Stackoverflow}

import scala.util.matching.Regex

trait LinkService[F[_]] {
  def getLinks(chatId: Long, tag: Option[String]): F[List[Link]]
  def addLink(chatId: Long, url: String, tags: Set[String]): F[Link]
  def deleteLink(chatId: Long, url: String): F[Link]
}

object LinkService {
  trait Error extends DomainError

  case class LinkAlreadyExist(uri: String) extends Error

  case object LinkNotFound extends Error

  case class UnexpectedLink(uri: String) extends Error

  private val githubUrl: Regex        = """https://api.github\.com/repos/(\w+)/(\w+)""".r
  private val stackoverflowUrl: Regex = """https://api.stackexchange\.com/questions/(\d+)/.*""".r

  def make[F[_]: Async](linkRepository: LinkRepository[F]): LinkService[F] = new LinkService[F] {
    override def getLinks(chatId: Long, tag: Option[String]): F[List[Link]] = tag match {
      case Some(tag) => linkRepository.getLinks(chatId, tag)
      case _         => linkRepository.getLinks(chatId)
    }

    override def addLink(chatId: Long, url: String, tags: Set[String]): F[Link] =
      mkLink(chatId, url, tags).liftTo[F].flatMap(linkRepository.insertLink(chatId, _))

    override def deleteLink(chatId: Long, url: String): F[Link] =
      linkRepository.deleteLink(chatId: Long, url: String)

    private def mkLink(chatId: Long, url: String, tags: Set[String]): Either[UnexpectedLink, Link] = {
      url match {
        case githubUrl(user, repo) =>
          Right(Link(
            url,
            apiUrl = s"https://api.github.com/repos/$user/$repo",
            tags = tags,
            chatIds = Set(chatId),
            site = Github
          ))

        case stackoverflowUrl(questionId) => Right(Link(
            url,
            apiUrl = s"https://api.stackexchange.com/2.3/questions/$questionId?site=stackoverflow",
            tags = tags,
            chatIds = Set(chatId),
            site = Stackoverflow
          ))

        case _ => Left(UnexpectedLink(url))
      }
    }
  }
}
