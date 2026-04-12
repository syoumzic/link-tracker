package tbank.academy.domain.service

import cats.data.ReaderT
import cats.effect.unsafe.implicits.global
import cats.effect.Ref
import cats.implicits._
import com.comcast.ip4s.{Host, Port}
import org.scalacheck.Gen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import sttp.model.Uri
import tbank.academy.App.AppT
import tbank.academy.config._
import tbank.academy.{Github, Link, Site, Stackoverflow}
import tbank.academy.domain.client.{BotClient, GithubClient, StackoverflowClient}
import tbank.academy.domain.repository.{ChatRepository, LinkRepository}

import scala.concurrent.duration._
import scala.collection.immutable.Queue
import scala.concurrent.duration.FiniteDuration

class MonitorSpec extends AnyFlatSpec with Matchers with ScalaCheckPropertyChecks {

  class mocks(updates: Map[Site, List[Link]], links: List[Link], ref: Ref[AppT, Queue[Link]]) {
    val botClient: BotClient[AppT] = (links: Link) => ref.update(_.enqueue(links))

    private def findOrNone(site: Site, link: Link): AppT[Option[Link]] =
      updates.get(site).filter(_.contains(link)).as(link).pure[AppT]

    val githubCrawler: GithubClient[AppT] =
      (link: Link, _: FiniteDuration) => findOrNone(Github, link)

    val stackoverflowCrawler: StackoverflowClient[AppT] =
      (link: Link, _: FiniteDuration) => findOrNone(Stackoverflow, link)

    val chatRepository: ChatRepository[AppT] = new ChatRepository[AppT] {
      override def registerChat(chatId: Long): AppT[Unit] = ReaderT.pure(())

      override def deleteChat(chatId: Long): AppT[Unit] = ReaderT.pure(())
    }

    val linkRepository: LinkRepository[AppT] = new LinkRepository[AppT] {
      private val emptyLink = Link("", "", Github, Set.empty[String], Set.empty[Long], None)

      override def getLinks(chatId: Long): AppT[List[Link]] = links.pure[AppT]

      override def updateLinks(links: List[Link]): AppT[Unit] = ReaderT.pure(())

      override def getLinks: AppT[List[Link]] = links.pure[AppT]

      override def insertLink(chatId: Long, link: Link): AppT[Link] = link.pure[AppT]

      override def getLinks(chatId: Long, tag: String): AppT[List[Link]] = List(
        emptyLink
      ).pure[AppT]

      override def deleteLink(chatId: Long, url: String): AppT[Link] = emptyLink.pure[AppT]
    }
  }

  case class LinksTemplate(links: List[Link], updates: Map[Site, List[Link]])

  implicit val uriGen: Gen[String] =
    Gen.oneOf("https://github.com/user/repo", "https://stackoverflow.com/questions/123", "http://example.com/path")

  implicit val tagGen: Gen[String] = Gen.oneOf("dev", "scala", "python", "angular", "lmao")

  implicit val linkGen: Gen[Link] = for {
    chatId <- Gen.long
    tags   <- Gen.listOf(tagGen)
    url    <- uriGen
    apiUrl <- uriGen
    site   <- Gen.oneOf(Github, Stackoverflow)
  } yield Link(
    url = url,
    apiUrl = apiUrl,
    site = site,
    tags = tags.toSet,
    chatIds = Set(chatId),
    lastUpdate = None
  )
  // scalafix:off
  val testConfig: AppConfig = AppConfig(
    server = ServerConfig(
      host = Host.fromString("localhost").get,
      port = Port.fromInt(8080).get
    ),
    monitor = MonitorConfig(
      timeout = 10.seconds
    ),
    bot = BotConfig(
      url = Uri.unsafeApply("http://localhost:8090")
    ),
    doobie = DoobieConfig(
      driver = "",
      user = "",
      url = "",
      password = ""
    ),
    `access-type` = ""
  )
  // scalafix:on

  implicit val templateGen: Gen[LinksTemplate] = for {
    links       <- Gen.listOf(linkGen)
    someOfLinks <- Gen.someOf(links)
  } yield LinksTemplate(links, someOfLinks.groupBy(_.site).view.mapValues(_.toList).toMap)

  "Monitor" should "send all updates to bot client" in forAll(templateGen) { template =>
    (for {
      ref <- Ref.of[AppT, Queue[Link]](Queue.empty)
      mocks   = new mocks(template.updates, template.links, ref)
      monitor =
        Monitor.make[AppT](mocks.linkRepository, mocks.botClient, mocks.githubCrawler, mocks.stackoverflowCrawler)
      _      <- monitor.run
      output <- ref.get
    } yield output.toSet shouldBe template.updates.values.flatten.toSet).run(testConfig).unsafeRunSync()
  }
}
