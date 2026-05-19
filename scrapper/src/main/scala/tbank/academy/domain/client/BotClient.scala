package tbank.academy.domain.client

import tbank.academy.Link

import java.time.ZonedDateTime

trait BotClient[F[_]] {
  def updatePost(link: Link): F[Unit]
  def updateIssue(
      chatIds: Set[Long],
      url: String,
      title: String,
      username: String,
      uptime: ZonedDateTime,
      description: String
  ): F[Unit]
  def updatePullRequest(
      chatIds: Set[Long],
      url: String,
      title: String,
      username: String,
      uptime: ZonedDateTime
  ): F[Unit]
  def updateComment(
      chatIds: Set[Long],
      question: String,
      username: String,
      uptime: ZonedDateTime,
      description: String
  ): F[Unit]
  def updateAnswer(
      chatIds: Set[Long],
      question: String,
      username: String,
      uptime: ZonedDateTime,
      description: String
  ): F[Unit]
}
