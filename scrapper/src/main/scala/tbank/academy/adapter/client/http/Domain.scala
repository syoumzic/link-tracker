package tbank.academy.adapter.client.http

import derevo.derive
import tbank.academy.Predef.tethysReaderSnake
import tethys.JsonReader
import tethys.derivation.builder.{FieldStyle, ReaderBuilder}
import tethys.derivation.semiauto.jsonReader

import java.time.ZonedDateTime
object Domain {

  object events {
    val pullRequest = "PullRequestEvent"
    val issue       = "IssuesEvent"
    val opened      = "opened"
  }

  case class GithubRequestItem(
      id: String,
      `type`: String,
      actor: GithubActor,
      repo: Repo,
      payload: GithubPayload,
      public: Boolean,
      createdAt: ZonedDateTime
  )

  implicit val githubRequestItemReader: JsonReader[GithubRequestItem] = jsonReader[GithubRequestItem] {
    ReaderBuilder[GithubRequestItem]
      .fieldStyle(FieldStyle.lowerSnakecase)
      .extractReader(_.payload).from(_.`type`) {
        case events.pullRequest => JsonReader[PullRequestPayload]
        case events.issue       => JsonReader[IssuesPayload]
        case _                  => JsonReader[UnknownPayload]
      }
  }

  @derive(tethysReaderSnake)
  case class GithubActor(
      id: Long,
      login: String,
      displayLogin: String,
      gravatarId: String,
      url: String,
      avatarUrl: String
  )

  sealed trait GithubPayload

  @derive(tethysReaderSnake)
  case class PullRequestPayload(action: String, number: Int, pullRequest: PullRequest) extends GithubPayload

  @derive(tethysReaderSnake)
  case class IssuesPayload(action: String, issue: Issue) extends GithubPayload

  @derive(tethysReaderSnake)
  case class UnknownPayload() extends GithubPayload

  @derive(tethysReaderSnake)
  case class PullRequest(url: String, id: Long, number: Int, head: Info, base: Info)

  @derive(tethysReaderSnake)
  case class Info(ref: String, sha: String, repo: Repo)

  @derive(tethysReaderSnake)
  case class Repo(id: Long, url: String, name: String)

  @derive(tethysReaderSnake)
  case class Issue(
      url: String,
      repositoryUrl: String,
      labelsUrl: String,
      commentsUrl: String,
      eventsUrl: String,
      htmlUrl: String,
      id: Long,
      nodeId: String,
      number: Int,
      title: String,
      user: User,
      labels: List[String],
      state: String,
      locked: Boolean,
      comments: Int,
      createdAt: ZonedDateTime,
      updatedAt: ZonedDateTime,
      issueDependenciesSummary: Summary,
      body: String,
      timelineUrl: String
  )

  @derive(tethysReaderSnake)
  case class Summary(blockedBy: Int, totalBlockedBy: Int, blocking: Int, totalBlocking: Int)

  @derive(tethysReaderSnake)
  case class User(
      login: String,
      id: Long,
      nodeId: String,
      avatarUrl: String,
      gravatarId: String,
      url: String,
      htmlUrl: String,
      followersUrl: String,
      followingUrl: String,
      gistsUrl: String,
      starredUrl: String,
      subscriptionsUrl: String,
      organizationsUrl: String,
      reposUrl: String,
      eventsUrl: String,
      receivedEventsUrl: String,
      `type`: String,
      userViewType: String,
      siteAdmin: Boolean,
  )

  @derive(tethysReaderSnake)
  case class StackoverflowResponse[A <: Item: JsonReader](
      items: List[A],
      hasMore: Boolean,
      quotaMax: Int,
      quotaRemaining: Int
  )

  sealed trait Item {
    def creationDate: Long
  }

  @derive(tethysReaderSnake)
  case class QuestionItem(
      tags: List[String],
      owner: Owner,
      isAnswered: Boolean,
      viewCount: Int,
      answerCount: Int,
      lastActivityDate: Long,
      creationDate: Long,
      questionId: Long,
      link: String,
      title: String
  ) extends Item

  @derive(tethysReaderSnake)
  case class Owner(
      reputation: Long,
      userId: Long,
      userType: String,
      profileImage: String,
      displayName: String,
      link: String
  )

  @derive(tethysReaderSnake)
  case class AnswerItem(
      owner: Owner,
      isAccepted: Boolean,
      score: Int,
      lastActivityDate: Long,
      lastEditDate: Long,
      creationDate: Long,
      answerId: Long,
      questionId: Long,
      body: String
  ) extends Item

  @derive(tethysReaderSnake)
  case class CommentItem(
      owner: Owner,
      edited: Boolean,
      score: Int,
      creationDate: Long,
      postId: Long,
      commentId: Long,
      body: String
  ) extends Item
}
