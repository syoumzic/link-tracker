package tbank.academy.domain.service

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalamock.scalatest.MockFactory
import org.scalatest.Assertion
import org.scalatest.flatspec.AnyFlatSpec
import sttp.client4.ResponseException.UnexpectedStatusCode
import sttp.model.{ResponseMetadata, StatusCode}
import tbank.academy.domain.client.{ApiClient, BotClient, StackoverflowBatchClient}
import tbank.academy.{Link, Stackoverflow}
import tbank.academy.domain.repository.LinkRepository
import tbank.academy.http.LinkUpdate.UpdateQuestion

import java.time.{Instant, ZoneId, ZonedDateTime}
import tbank.academy.adapter.client.http.Domain.{AnswerItem, CommentItem, QuestionItem, StackoverflowResponse}
import tethys.JsonReader
import tofu.logging.Logging

import scala.annotation.nowarn
import scala.collection.mutable

//scalafix:off Disable.collection.mutable
@nowarn("msg=unused value")
class StackOverflowAnalyzerSpec extends AnyFlatSpec with MockFactory with AnalyzerAbstract {
  val url                = "<url>"
  val descriptionSize    = 15
  val chatIds: Set[Long] = Set(1L, 2L)
  val maxConcurrent      = 4
  val batchSize          = 999

  val repo: LinkRepository[IO]          = mock[LinkRepository[IO]]
  val client: ApiClient[IO]             = mock[ApiClient[IO]]
  val botClient: BotClient[IO]          = mock[BotClient[IO]]
  implicit val logger: Logging.Make[IO] = Logging.Make.plain[IO]

  def stackoverflowTest(
      updateAnswer: Set[UpdateQuestion] = Set.empty,
      updateComment: Set[UpdateQuestion] = Set.empty,
      processedCount: Long = 0
  ): Assertion = {
    (() => repo.getLinks)
      .expects()
      .returning(IO(List(Link(
        chatIds = chatIds,
        url = url,
        apiUrl = url,
        site = Stackoverflow,
        processedCount = processedCount,
      ))))

    (repo.updateCount _)
      .expects(*, *)
      .returning(IO.unit)

    (client.execute[StackoverflowResponse[QuestionItem]](_: String)(_: JsonReader[StackoverflowResponse[QuestionItem]]))
      .expects(*, *)
      .returning(getJson[StackoverflowResponse[QuestionItem]]("stackoverflow/questions/Ok"))

    (client.execute[StackoverflowResponse[AnswerItem]](_: String)(_: JsonReader[StackoverflowResponse[AnswerItem]]))
      .expects(*, *)
      .returning(getJson[StackoverflowResponse[AnswerItem]]("stackoverflow/questions/answers/Ok"))

    (client.execute[StackoverflowResponse[CommentItem]](_: String)(_: JsonReader[StackoverflowResponse[CommentItem]]))
      .expects(*, *)
      .returning(getJson[StackoverflowResponse[CommentItem]]("stackoverflow/questions/comments/Ok"))

    val updateAnswerQueue: mutable.Queue[UpdateQuestion] = mutable.Queue.empty

    (botClient.updateAnswer _)
      .expects(*, *, *, *, *)
      .onCall((chatIds, question, username, uptime, description) => {
        updateAnswerQueue.enqueue(UpdateQuestion(chatIds, question, username, uptime, description))
        IO.unit
      })

    val updateCommentQueue: mutable.Queue[UpdateQuestion] = mutable.Queue.empty

    (botClient.updateComment _)
      .expects(*, *, *, *, *)
      .onCall((chatIds, question, username, uptime, description) => {
        updateCommentQueue.enqueue(UpdateQuestion(chatIds, question, username, uptime, description))
        IO.unit
      })

    val stackOverflowClient = StackoverflowBatchClient.makeInternal(client)(batchSize)

    StackoverflowAnalyzer.makeInternal(stackOverflowClient, botClient, repo)(maxConcurrent, 0L).update.unsafeRunSync()

    assert(updateAnswerQueue.toSet == updateAnswer && updateCommentQueue.toSet == updateComment)
  }

  "stackoverflow analyzer" should "send commands to bot" in stackoverflowTest(
    updateAnswer = Set(
      UpdateQuestion(
        chatIds = chatIds,
        question = "How do I determine the state of a VBA workbook before attemting to close it (Excel 2007)?",
        username = "Tim Williams",
        uptime = ZonedDateTime.ofInstant(
          Instant.ofEpochMilli(1777393667),
          ZoneId.of("UTC")
        ),
        description = "1"
      )
    ),
    updateComment = Set(
      UpdateQuestion(
        chatIds = chatIds,
        question = "How do I determine the state of a VBA workbook before attemting to close it (Excel 2007)?",
        username = "Tim Williams",
        uptime = ZonedDateTime.ofInstant(
          Instant.ofEpochMilli(1777393333),
          ZoneId.of("UTC")
        ),
        description = "2"
      )
    )
  )

  "stackoverflow analyzer" should "skip link with bad status" in {
    val goodUrl = "<good-url>"
    val badUrl  = "<bad-url>"

    (() => repo.getLinks)
      .expects()
      .returning(IO(List(
        Link(chatIds = chatIds, url = badUrl, apiUrl = badUrl, site = Stackoverflow, processedCount = 0),
        Link(chatIds = chatIds, url = goodUrl, apiUrl = goodUrl, site = Stackoverflow, processedCount = 0),
      )))

    (repo.updateCount _)
      .expects(goodUrl, *)
      .returning(IO.unit)

    (client.execute[StackoverflowResponse[QuestionItem]](_: String)(_: JsonReader[StackoverflowResponse[QuestionItem]]))
      .expects(where { (url: String, _: Any) => url.startsWith(badUrl) })
      .returning(IO.raiseError(UnexpectedStatusCode[String](
        "error",
        ResponseMetadata(StatusCode.BadRequest, "", Seq.empty)
      )))

    (client.execute[StackoverflowResponse[QuestionItem]](_: String)(_: JsonReader[StackoverflowResponse[QuestionItem]]))
      .expects(where { (url: String, _: Any) => url.startsWith(goodUrl) })
      .returning(getJson[StackoverflowResponse[QuestionItem]]("stackoverflow/questions/Ok"))

    (client.execute[StackoverflowResponse[AnswerItem]](_: String)(_: JsonReader[StackoverflowResponse[AnswerItem]]))
      .expects(where { (url: String, _: Any) => url.startsWith(goodUrl) })
      .returning(getJson[StackoverflowResponse[AnswerItem]]("stackoverflow/questions/answers/Ok"))

    (client.execute[StackoverflowResponse[CommentItem]](_: String)(_: JsonReader[StackoverflowResponse[CommentItem]]))
      .expects(where { (url: String, _: Any) => url.startsWith(goodUrl) })
      .returning(getJson[StackoverflowResponse[CommentItem]]("stackoverflow/questions/comments/Ok"))

    val updateAnswerQueue: mutable.Queue[UpdateQuestion] = mutable.Queue.empty

    (botClient.updateAnswer _)
      .expects(*, *, *, *, *)
      .onCall((chatIds, question, username, uptime, description) => {
        updateAnswerQueue.enqueue(UpdateQuestion(chatIds, question, username, uptime, description))
        IO.unit
      })

    val updateCommentQueue: mutable.Queue[UpdateQuestion] = mutable.Queue.empty

    (botClient.updateComment _)
      .expects(*, *, *, *, *)
      .onCall((chatIds, question, username, uptime, description) => {
        updateCommentQueue.enqueue(UpdateQuestion(chatIds, question, username, uptime, description))
        IO.unit
      })

    val stackOverflowClient = StackoverflowBatchClient.makeInternal(client)(batchSize)

    StackoverflowAnalyzer.makeInternal(stackOverflowClient, botClient, repo)(maxConcurrent, 0L).update.unsafeRunSync()

    assert(updateAnswerQueue.nonEmpty && updateCommentQueue.nonEmpty)
  }
}
//scalafix:on Disable.collection.mutable
