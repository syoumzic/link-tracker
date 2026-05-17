package tbank.academy.adapter.client.kafka

import cats.effect.Async
import tbank.academy.Link
import tbank.academy.adapter.kafka.KafkaProducerService
import tbank.academy.domain.client.BotClient
import tbank.academy.kafka.KafkaMessage

import java.time.ZonedDateTime

object KafkaBotClient {
  def make[F[_]: Async](kafkaProducerService: KafkaProducerService[F]): BotClient[F] =
    new BotClient[F] {
      override def updatePost(link: Link): F[Unit] = Async[F].unit

      override def updatePullRequest(
          chatIds: Set[Long],
          url: String,
          title: String,
          username: String,
          uptime: ZonedDateTime
      ): F[Unit] =
        kafkaProducerService.producePullRequest(KafkaMessage.PullRequestUpdate(
          chatIds = chatIds,
          url = url,
          title = title,
          username = username,
          uptime = uptime
        ))

      override def updateIssue(
          chatIds: Set[Long],
          url: String,
          title: String,
          username: String,
          uptime: ZonedDateTime,
          description: String
      ): F[Unit] =
        kafkaProducerService.produceIssue(KafkaMessage.IssueUpdate(
          chatIds = chatIds,
          url = url,
          title = title,
          username = username,
          uptime = uptime,
          description = description
        ))

      override def updateComment(
          chatIds: Set[Long],
          question: String,
          username: String,
          uptime: ZonedDateTime,
          description: String
      ): F[Unit] =
        kafkaProducerService.produceComment(KafkaMessage.CommentUpdate(
          chatIds = chatIds,
          question = question,
          username = username,
          uptime = uptime,
          description = description
        ))

      override def updateAnswer(
          chatIds: Set[Long],
          question: String,
          username: String,
          uptime: ZonedDateTime,
          description: String
      ): F[Unit] =
        kafkaProducerService.produceAnswer(KafkaMessage.AnswerUpdate(
          chatIds = chatIds,
          question = question,
          username = username,
          uptime = uptime,
          description = description
        ))
    }
}
