package tbank.academy.adapter.client.http

import cats.data.NonEmptyList

import java.time.Instant

case class StackoverflowGetRequest(items: NonEmptyList[StackItem])

case class StackItem(lastEditDate: Instant)

private case class GithubRequest(updatedAt: Instant)
