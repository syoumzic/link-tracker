package tbank.academy.adapter.kafka

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.KafkaContainer
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import tbank.academy.config.{KafkaConfig, KafkaConsumerConfig, KafkaProducerConfig, KafkaTopicsConfig}
import tbank.academy.domain.telegram.TgService
import tbank.academy.kafka.KafkaMessage._
import cats.implicits._

import java.time.ZonedDateTime
import scala.collection.mutable
import scala.concurrent.duration.DurationInt
import annotation.nowarn

// scalafix:off DisableSyntax.var
// scalafix:off Disable.collection.mutable
// scalafix:off DisableSyntax.isInstanceOf
@nowarn("msg=unused")
class KafkaSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll with MockFactory {

  private var kafkaContainer: KafkaContainer = _
  val tgService: TgService[IO]               = mock[TgService[IO]]

  override def beforeAll(): Unit = {
    kafkaContainer = KafkaContainer()
    kafkaContainer.start()
  }

  override def afterAll(): Unit = {
    kafkaContainer.stop()
  }

  "KafkaProducerService and KafkaConsumerService" should "deliver pull request messages end-to-end" in {
    val bootstrapServers = kafkaContainer.bootstrapServers

    val kafkaConfig = KafkaConfig(
      enabled = true,
      bootstrapServers = List(bootstrapServers),
      topics = KafkaTopicsConfig(
        pullRequest = "test-pr-topic",
        issue = "test-issue-topic",
        comment = "test-comment-topic",
        answer = "test-answer-topic"
      ),
      producer = KafkaProducerConfig(acks = "1", retries = 3, batchSize = 16384),
      consumer = KafkaConsumerConfig(
        groupId = "test-consumer-group",
        autoOffsetReset = "earliest",
        enableAutoCommit = true,
        maxPollRecords = 100,
        pollTimeoutMs = 100
      )
    )

    val receivedMessages = mutable.Queue.empty[PullRequestUpdate]

    (tgService.updatePullRequest _)
      .expects(*, *, *, *, *)
      .onCall { (chatId, url, title, username, uptime) =>
        receivedMessages.enqueue(PullRequestUpdate(Set(chatId), url, title, username, uptime)).pure[IO].void
      }
      .twice()

    val testMessage = PullRequestUpdate(
      chatIds = Set(123L, 456L),
      url = "https://github.com/test/repo/pull/42",
      title = "Test PR Title",
      username = "test-user",
      uptime = ZonedDateTime.parse("2026-04-19T12:35:06Z")
    )

    KafkaProducerService.make[IO](kafkaConfig).use { producerService =>
      KafkaConsumerService.make[IO](kafkaConfig, tgService).use { consumerService =>
        for {
          _ <- producerService.producePullRequest(testMessage)
          _ <- consumerService.consumeStream
            .timeout(10.seconds)
            .compile
            .drain
            .attempt
        } yield ()
      }
    }.unsafeRunSync()

    Thread.sleep(500)

    val received1 = receivedMessages.dequeue()
    val received2 = receivedMessages.dequeue()
    received1.title shouldBe "Test PR Title"
    received1.username shouldBe "test-user"
    received2.title shouldBe "Test PR Title"
    received2.username shouldBe "test-user"
    (received1.chatIds ++ received2.chatIds) should contain allElementsOf Set(123L, 456L)
  }

  it should "deliver all message types correctly" in {
    val bootstrapServers = kafkaContainer.bootstrapServers

    val kafkaConfig = KafkaConfig(
      enabled = true,
      bootstrapServers = List(bootstrapServers),
      topics = KafkaTopicsConfig(
        pullRequest = "test-all-pr",
        issue = "test-all-issue",
        comment = "test-all-comment",
        answer = "test-all-answer"
      ),
      producer = KafkaProducerConfig(acks = "1", retries = 3, batchSize = 16384),
      consumer = KafkaConsumerConfig(
        groupId = "test-all-group",
        autoOffsetReset = "earliest",
        enableAutoCommit = true,
        maxPollRecords = 100,
        pollTimeoutMs = 100
      )
    )

    val receivedMessages = mutable.Queue.empty[Any]

    (tgService.updatePullRequest _).expects(*, *, *, *, *).onCall { (chatId, url, title, username, uptime) =>
      receivedMessages.enqueue(PullRequestUpdate(Set(chatId), url, title, username, uptime))
      IO.unit
    }
    (tgService.updateIssue _).expects(*, *, *, *, *, *).onCall { (chatId, url, title, username, uptime, description) =>
      receivedMessages.enqueue(IssueUpdate(Set(chatId), url, title, username, uptime, description))
      IO.unit
    }
    (tgService.updateComment _).expects(*, *, *, *, *).onCall { (chatId, question, username, uptime, description) =>
      receivedMessages.enqueue(CommentUpdate(Set(chatId), question, username, uptime, description))
      IO.unit
    }
    (tgService.updateAnswer _).expects(*, *, *, *, *).onCall { (chatId, question, username, uptime, description) =>
      receivedMessages.enqueue(AnswerUpdate(Set(chatId), question, username, uptime, description))
      IO.unit
    }

    val prMessage = PullRequestUpdate(
      chatIds = Set(1L),
      url = "https://github.com/test/pr",
      title = "PR Title",
      username = "pr-user",
      uptime = ZonedDateTime.now()
    )
    val issueMessage = IssueUpdate(
      chatIds = Set(2L),
      url = "https://github.com/test/issue",
      title = "Issue Title",
      username = "issue-user",
      uptime = ZonedDateTime.now(),
      description = "Issue description"
    )
    val commentMessage = CommentUpdate(
      chatIds = Set(3L),
      question = "Test question?",
      username = "comment-user",
      uptime = ZonedDateTime.now(),
      description = "Comment description"
    )
    val answerMessage = AnswerUpdate(
      chatIds = Set(4L),
      question = "Test question?",
      username = "answer-user",
      uptime = ZonedDateTime.now(),
      description = "Answer description"
    )

    KafkaProducerService.make[IO](kafkaConfig).use { producerService =>
      KafkaConsumerService.make[IO](kafkaConfig, tgService).use { consumerService =>
        for {
          _ <- producerService.producePullRequest(prMessage)
          _ <- producerService.produceIssue(issueMessage)
          _ <- producerService.produceComment(commentMessage)
          _ <- producerService.produceAnswer(answerMessage)
          _ <- consumerService.consumeStream
            .timeout(10.seconds)
            .compile
            .drain
            .attempt
        } yield ()
      }
    }.unsafeRunSync()

    Thread.sleep(500)

    receivedMessages.size shouldBe 4
    receivedMessages.exists(_.isInstanceOf[PullRequestUpdate]) shouldBe true
    receivedMessages.exists(_.isInstanceOf[IssueUpdate]) shouldBe true
    receivedMessages.exists(_.isInstanceOf[CommentUpdate]) shouldBe true
    receivedMessages.exists(_.isInstanceOf[AnswerUpdate]) shouldBe true
  }

  it should "preserve message content through serialization/deserialization" in {
    val bootstrapServers = kafkaContainer.bootstrapServers

    val kafkaConfig = KafkaConfig(
      enabled = true,
      bootstrapServers = List(bootstrapServers),
      topics = KafkaTopicsConfig(
        pullRequest = "test-serialize-pr",
        issue = "test-serialize-issue",
        comment = "test-serialize-comment",
        answer = "test-serialize-answer"
      ),
      producer = KafkaProducerConfig(acks = "1", retries = 3, batchSize = 16384),
      consumer = KafkaConsumerConfig(
        groupId = "test-serialize-group",
        autoOffsetReset = "earliest",
        enableAutoCommit = true,
        maxPollRecords = 100,
        pollTimeoutMs = 100
      )
    )

    val receivedMessages = mutable.Queue.empty[IssueUpdate]

    (tgService.updateIssue _).expects(*, *, *, *, *, *).onCall {
      (chatId: Long, url: String, title: String, username: String, uptime: ZonedDateTime, description: String) =>
        receivedMessages.enqueue(IssueUpdate(Set(chatId), url, title, username, uptime, description))
        IO.unit
    }.repeat(3)
    (tgService.updatePullRequest _).expects(*, *, *, *, *).returning(IO.unit).anyNumberOfTimes()
    (tgService.updateComment _).expects(*, *, *, *, *).returning(IO.unit).anyNumberOfTimes()
    (tgService.updateAnswer _).expects(*, *, *, *, *).returning(IO.unit).anyNumberOfTimes()

    val originalMessage = IssueUpdate(
      chatIds = Set(100L, 200L, 300L),
      url = "https://github.com/complex/repo/issues/999",
      title = "Complex Issue with Special Chars: äöü ñ 中文",
      username = "complex-user_123",
      uptime = ZonedDateTime.parse("2026-01-15T08:30:00Z"),
      description = "Description with\nnewlines\tand special chars: !@#$%^&*()"
    )

    KafkaProducerService.make[IO](kafkaConfig).use { producerService =>
      KafkaConsumerService.make[IO](kafkaConfig, tgService).use { consumerService =>
        for {
          _ <- producerService.produceIssue(originalMessage)
          _ <- consumerService.consumeStream
            .timeout(10.seconds)
            .compile
            .drain
            .attempt
        } yield ()
      }
    }.unsafeRunSync()

    Thread.sleep(500)

    receivedMessages.size shouldBe 3
    val allChatIds = receivedMessages.flatMap(_.chatIds)
    allChatIds should contain allElementsOf originalMessage.chatIds

    val received = receivedMessages.head
    received.url shouldBe originalMessage.url
    received.title shouldBe originalMessage.title
    received.username shouldBe originalMessage.username
    received.uptime shouldBe originalMessage.uptime
    received.description shouldBe originalMessage.description
  }
}
// scalafix:on DisableSyntax.var
// scalafix:on Disable.collection.mutable
// scalafix:on DisableSyntax.isInstanceOf
