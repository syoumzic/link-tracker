package tbank.academy.adapter.kafka

import cats.effect.{Async, Resource, Sync}
import cats.implicits._
import fs2.Stream
import org.apache.kafka.clients.consumer.{ConsumerConfig, KafkaConsumer => JKafkaConsumer}
import tbank.academy.config.{AppConfig, KafkaConfig}
import tbank.academy.domain.telegram.TgService
import tbank.academy.kafka.KafkaMessage
import tofu.WithContext
import tethys._
import tethys.jackson._

import java.util.Properties
import scala.jdk.CollectionConverters._

trait KafkaConsumerService[F[_]] {
  def consumeStream: Stream[F, Unit]
}

object KafkaConsumerService {
  def make[F[_]: Async](kafkaConfig: KafkaConfig, tgService: TgService[F]): Resource[F, KafkaConsumerService[F]] = {
    def deserializePR(data: String): F[Any] =
      data.jsonAs[KafkaMessage.PullRequestUpdate].liftTo[F].widen[Any]

    def deserializeIssue(data: String): F[Any] =
      data.jsonAs[KafkaMessage.IssueUpdate].liftTo[F].widen[Any]

    def deserializeComment(data: String): F[Any] =
      data.jsonAs[KafkaMessage.CommentUpdate].liftTo[F].widen[Any]

    def deserializeAnswer(data: String): F[Any] =
      data.jsonAs[KafkaMessage.AnswerUpdate].liftTo[F].widen[Any]

    Resource.pure(new KafkaConsumerService[F] {
      override def consumeStream: Stream[F, Unit] = {
        val prStream      = createStream(kafkaConfig, kafkaConfig.topics.pullRequest, "pr", deserializePR, tgService)
        val issueStream   = createStream(kafkaConfig, kafkaConfig.topics.issue, "issue", deserializeIssue, tgService)
        val commentStream =
          createStream(kafkaConfig, kafkaConfig.topics.comment, "comment", deserializeComment, tgService)
        val answerStream = createStream(kafkaConfig, kafkaConfig.topics.answer, "answer", deserializeAnswer, tgService)

        prStream.merge(issueStream).merge(commentStream).merge(answerStream)
      }
    })
  }

  def make[F[_]: Async](tgService: TgService[F])(implicit
      context: WithContext[F, AppConfig]
  ): Resource[F, KafkaConsumerService[F]] = {
    Resource.eval(context.ask(_.kafka)).flatMap(kafkaConfig => make[F](kafkaConfig, tgService))
  }

  private def createStream[F[_]: Async](
      kafkaConfig: KafkaConfig,
      topic: String,
      groupIdSuffix: String,
      deserialize: String => F[Any],
      tgService: TgService[F]
  ): Stream[F, Unit] = {
    val props = new Properties()
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.bootstrapServers.mkString(","))
    props.put(ConsumerConfig.GROUP_ID_CONFIG, s"${kafkaConfig.consumer.groupId}-$groupIdSuffix")
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, kafkaConfig.consumer.autoOffsetReset)
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, kafkaConfig.consumer.enableAutoCommit.toString)
    props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, kafkaConfig.consumer.maxPollRecords.toString)
    props.put(ConsumerConfig.CLIENT_ID_CONFIG, s"link-tracker-bot-$groupIdSuffix")

    Stream
      .resource(Resource.make(
        Sync[F].delay(new JKafkaConsumer[String, String](
          props,
          new org.apache.kafka.common.serialization.StringDeserializer,
          new org.apache.kafka.common.serialization.StringDeserializer
        ))
      )(c => Sync[F].delay(c.close())))
      .flatMap { consumer =>
        Stream.exec(Sync[F].delay(consumer.subscribe(java.util.Collections.singletonList(topic)))) ++
          Stream
            .repeatEval {
              Sync[F].delay {
                val records = consumer.poll(java.time.Duration.ofMillis(kafkaConfig.consumer.pollTimeoutMs))
                records.iterator().asScala.toList
              }
            }
            .flatMap(os => Stream.emits(os))
            .evalMap { record =>
              deserialize(record.value()).flatMap(processMessage(_, tgService))
            }
      }
  }

  private def processMessage[F[_]: Async](message: Any, tgService: TgService[F]): F[Unit] = {
    message match {
      case pr: KafkaMessage.PullRequestUpdate =>
        pr.chatIds.toList.traverse_(cid => tgService.updatePullRequest(cid, pr.url, pr.title, pr.username, pr.uptime))
      case issue: KafkaMessage.IssueUpdate =>
        issue.chatIds.toList.traverse_(cid =>
          tgService.updateIssue(cid, issue.url, issue.title, issue.username, issue.uptime, issue.description)
        )
      case comment: KafkaMessage.CommentUpdate =>
        comment.chatIds.toList.traverse_(cid =>
          tgService.updateComment(cid, comment.question, comment.username, comment.uptime, comment.description)
        )
      case answer: KafkaMessage.AnswerUpdate =>
        answer.chatIds.toList.traverse_(cid =>
          tgService.updateAnswer(cid, answer.question, answer.username, answer.uptime, answer.description)
        )
      case _ => Async[F].unit
    }
  }.void
}
