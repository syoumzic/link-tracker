package tbank.academy.domain.service

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Ref}
import cats.implicits._
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sttp.model.Uri
import tbank.academy.domain.client.{BotClient, GithubClient, StackoverflowClient}
import tbank.academy.domain.model.TgChat.Id
import tbank.academy.domain.model.{Link, Site, TgChat}
import tbank.academy.domain.repository.{ChatRepository, LinkRepository}

import scala.collection.immutable.Queue

class MonitorSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  class mocks(updates: Map[Site, List[Link]], links: List[Link], ref: Ref[IO, Queue[Link]]) {
    val botClient: BotClient[IO] = (links: Link) => ref.update(_.enqueue(links))

    private def findOrNone(site: Site, link: Link): IO[Option[Link]] =
      updates.get(site).filter(_.contains(link)).as(link).pure[IO]

    val githubCrawler: GithubClient[IO] = new GithubClient[IO] {
      override val site: Site = Site.Github

      override def requestUpdate(link: Link): IO[Option[Link]] = findOrNone(Site.Github, link)
    }

    val stackoverflowCrawler: StackoverflowClient[IO] = new StackoverflowClient[IO] {
      override val site: Site = Site.StackOverflow

      override def requestUpdate(link: Link): IO[Option[Link]] = findOrNone(Site.StackOverflow, link)
    }

    val chatRepository: ChatRepository[IO] = new ChatRepository[IO] {
      override def registerChat(chatId: TgChat.Id): IO[Unit] = IO.unit

      override def deleteChat(chatId: TgChat.Id): IO[Unit] = IO.unit

      override def getLinks(chatId: TgChat.Id, tag: Option[Link.Tag]): IO[List[Link]] = links.pure[IO]

      override def deleteLink(chatId: TgChat.Id, uri: String): IO[Link] =
        Link(chatId, List.empty, Uri(uri), Uri(uri), Site.Github).pure[IO]

      override def addLink(chatId: Id, link: Link): IO[Link] = link.pure[IO]
    }

    val linkRepository: LinkRepository[IO] = new LinkRepository[IO] {
      override def getLinks(chatId: TgChat.Id): IO[List[Link]] = links.pure[IO]

      override def updateLinks(links: List[Link]): IO[Unit] = IO.unit

      override def getLinks: IO[List[Link]] = links.pure[IO]
    }
  }

  case class LinksTemplate(links: List[Link], updates: Map[Site, List[Link]])

  implicit val uriGen: Gen[Uri] = for {
    link <-
      Gen.oneOf("https://github.com/user/repo", "https://stackoverflow.com/questions/123", "http://example.com/path")
  } yield Uri(link)

  implicit val tagGen: Gen[Link.Tag] = Gen.oneOf("dev", "scala", "python", "angular", "lmao")

  implicit val linkGen: Gen[Link] = for {
    chatId <- Gen.long
    tags   <- Gen.listOf(tagGen)
    uri    <- uriGen
    apiUri <- uriGen
    site   <- Gen.oneOf(Site.Github, Site.StackOverflow)
  } yield Link(chatId, tags, uri, apiUri, site)

  implicit val templateGen: Gen[LinksTemplate] = for {
    links       <- Gen.listOf(linkGen)
    someOfLinks <- Gen.someOf(links)
  } yield LinksTemplate(links, someOfLinks.groupBy(_.site).view.mapValues(_.toList).toMap)

  "Monitor" should "send all updates to bot client" in forAll(templateGen) { template =>
    (for {
      ref <- Ref.of[IO, Queue[Link]](Queue.empty)
      mocks   = new mocks(template.updates, template.links, ref)
      monitor =
        Monitor.make[IO](mocks.linkRepository, mocks.botClient, List(mocks.githubCrawler, mocks.stackoverflowCrawler))
      _      <- monitor.run
      output <- ref.get
    } yield output.toSet shouldBe template.updates.values.flatten.toSet).unsafeRunSync()
  }
}
