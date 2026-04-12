package tbank.academy.domain.service

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalamock.scalatest.MockFactory
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import sttp.client4.ResponseException.UnexpectedStatusCode
import sttp.model.{ResponseMetadata, StatusCode}
import tbank.academy.domain.client.{ApiClient, BotClient, GithubBatchClient}
import tbank.academy.{Github, Link}
import tbank.academy.domain.repository.LinkRepository
import tbank.academy.http.LinkUpdate.{UpdateIssue, UpdatePullRequest}

import java.time.ZonedDateTime
import tbank.academy.adapter.client.http.Domain.GithubRequestItem
import tethys.JsonReader
import tofu.logging.Logging

import scala.annotation.nowarn
import scala.collection.mutable

// scalafix:off Disable.collection.mutable
@nowarn("msg=unused value")
class GithubAnalyzerSpec extends AnyFlatSpec with MockFactory with AnalyzerAbstract {
  val url                = "<some url>"
  val descriptionSize    = 999
  val chatIds: Set[Long] = Set(1L, 2L, 3L)
  val maxConcurrent      = 4
  val batchSize          = 999

  val repo: LinkRepository[IO]          = mock[LinkRepository[IO]]
  val client: ApiClient[IO]             = mock[ApiClient[IO]]
  val botClient: BotClient[IO]          = mock[BotClient[IO]]
  implicit val logger: Logging.Make[IO] = Logging.Make.plain[IO]

  def githubTest(
      input: String,
      updatePullRequest: Set[UpdatePullRequest] = Set.empty,
      updateIssue: Set[UpdateIssue] = Set.empty,
      processedCount: Long = 0
  ): Assertion = {
    (() => repo.getLinks)
      .expects()
      .returning(IO(List(Link(
        url = url,
        apiUrl = url,
        site = Github,
        processedCount = processedCount,
        chatIds = chatIds
      ))))

    (repo.updateCount _)
      .expects(*, *)
      .returning(IO.unit)

    (client.execute[List[GithubRequestItem]](_: String)(_: JsonReader[List[GithubRequestItem]]))
      .expects(*, *)
      .returning(getJson[List[GithubRequestItem]](input))

    val updatePullRequestQueue: mutable.Queue[UpdatePullRequest] = mutable.Queue.empty

    (botClient.updatePullRequest _)
      .expects(*, *, *, *, *)
      .onCall((chatIds, url, title, username, uptime) => {
        updatePullRequestQueue.enqueue(UpdatePullRequest(chatIds, url, title, username, uptime))
        IO.unit
      })

    val updateIssueQueue: mutable.Queue[UpdateIssue] = mutable.Queue.empty

    (botClient.updateIssue _)
      .expects(*, *, *, *, *, *)
      .onCall((chatIds, url, title, description, username, uptime) => {
        updateIssueQueue.enqueue(UpdateIssue(chatIds, url, title, description, username, uptime))
        IO.unit
      })

    val githubBatchClient = GithubBatchClient.makeInternal(client)(batchSize)

    GithubAnalyzer.makeInternal(githubBatchClient, botClient, repo)(maxConcurrent, 0L).update.unsafeRunSync()

    assert(updatePullRequestQueue.toSet == updatePullRequest && updateIssueQueue.toSet == updateIssue)
  }

  "github analyzer" should "send commands to bot" in githubTest(
    input = "github/repos/events/Ok",
    updateIssue = Set(
      UpdateIssue(
        chatIds = chatIds,
        url = url,
        username = "syoumzic",
        title = "test-issue",
        description = "test-message",
        uptime = ZonedDateTime.parse("2026-04-19T12:35:06Z")
      )
    ),
    updatePullRequest = Set(
      UpdatePullRequest(
        chatIds = chatIds,
        url = url,
        title = "test-github-api",
        username = "syoumzic",
        uptime = ZonedDateTime.parse("2026-04-19T12:41:55Z")
      )
    )
  )

  "github analyzer" should "skip link with bad status" in {
    val goodUrl = "<good-url>"
    val badUrl  = "<bad-url>"

    (() => repo.getLinks)
      .expects()
      .returning(IO(List(
        Link(chatIds = chatIds, url = badUrl, apiUrl = badUrl, site = Github, processedCount = 0),
        Link(chatIds = chatIds, url = goodUrl, apiUrl = goodUrl, site = Github, processedCount = 0),
      )))

    (repo.updateCount _)
      .expects(goodUrl, *)
      .returning(IO.unit)

    (client.execute[List[GithubRequestItem]](_: String)(_: JsonReader[List[GithubRequestItem]]))
      .expects(where { (url: String, _: Any) => url.startsWith(badUrl) })
      .returning(IO.raiseError(UnexpectedStatusCode[String](
        "error",
        ResponseMetadata(StatusCode.BadRequest, "", Seq.empty)
      )))

    (client.execute[List[GithubRequestItem]](_: String)(_: JsonReader[List[GithubRequestItem]]))
      .expects(where { (url: String, _: Any) => url.startsWith(goodUrl) })
      .returning(getJson[List[GithubRequestItem]]("github/repos/events/Ok"))

    val updatePullRequestQueue: mutable.Queue[UpdatePullRequest] = mutable.Queue.empty

    (botClient.updatePullRequest _)
      .expects(*, *, *, *, *)
      .onCall((chatIds, url, title, username, uptime) => {
        updatePullRequestQueue.enqueue(UpdatePullRequest(chatIds, url, title, username, uptime))
        IO.unit
      })

    val updateIssueQueue: mutable.Queue[UpdateIssue] = mutable.Queue.empty

    (botClient.updateIssue _)
      .expects(*, *, *, *, *, *)
      .onCall((chatIds, url, title, description, username, uptime) => {
        updateIssueQueue.enqueue(UpdateIssue(chatIds, url, title, description, username, uptime))
        IO.unit
      })

    val githubBatchClient = GithubBatchClient.makeInternal(client)(batchSize)

    GithubAnalyzer.makeInternal(githubBatchClient, botClient, repo)(maxConcurrent, 0L).update.unsafeRunSync()

    assert(updatePullRequestQueue.nonEmpty || updateIssueQueue.nonEmpty)
  }
}
// scalafix:on Disable.collection.mutable
