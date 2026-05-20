package tbank.academy.kafka

import derevo.derive
import derevo.tethys.{tethysReader, tethysWriter}

import java.time.ZonedDateTime

object KafkaMessage {
  @derive(tethysReader, tethysWriter)
  case class PullRequestUpdate(
      chatIds: Set[Long],
      url: String,
      title: String,
      username: String,
      uptime: ZonedDateTime
  )

  @derive(tethysReader, tethysWriter)
  case class IssueUpdate(
      chatIds: Set[Long],
      url: String,
      title: String,
      username: String,
      uptime: ZonedDateTime,
      description: String
  )

  @derive(tethysReader, tethysWriter)
  case class CommentUpdate(
      chatIds: Set[Long],
      question: String,
      username: String,
      uptime: ZonedDateTime,
      description: String
  )

  @derive(tethysReader, tethysWriter)
  case class AnswerUpdate(
      chatIds: Set[Long],
      question: String,
      username: String,
      uptime: ZonedDateTime,
      description: String
  )
}
