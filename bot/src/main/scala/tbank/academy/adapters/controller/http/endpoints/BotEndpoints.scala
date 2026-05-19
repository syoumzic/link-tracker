package tbank.academy.adapters.controller.http.endpoints

import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.tethys.jsonBody
import tbank.academy.http.LinkUpdate._
import tbank.academy.http._

object BotEndpoints {
  private val updates = "updates"

  val updatesPost: PublicEndpoint[LinkUpdate, (StatusCode, ApiErrorResponse), Unit, Any] = {
    // deprecated
    endpoint.post
      .summary("Отправить обновление")
      .in(updates)
      .in(jsonBody[LinkUpdate])
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))
  }

  val updatePullRequest: PublicEndpoint[UpdatePullRequest, (StatusCode, ApiErrorResponse), Unit, Any] =
    endpoint.post
      .summary("Обновление в Pull Request")
      .in("updatePullRequest")
      .in(jsonBody[UpdatePullRequest])
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val updateIssue: PublicEndpoint[UpdateIssue, (StatusCode, ApiErrorResponse), Unit, Any] =
    endpoint.post
      .summary("Обновление в Issue")
      .in("updateIssue")
      .in(jsonBody[UpdateIssue])
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val updateComment: PublicEndpoint[UpdateQuestion, (StatusCode, ApiErrorResponse), Unit, Any] =
    endpoint.post
      .summary("Обновление в комментарий Stackoverflow")
      .in("updateComment")
      .in(jsonBody[UpdateQuestion])
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val updateAnswer: PublicEndpoint[UpdateQuestion, (StatusCode, ApiErrorResponse), Unit, Any] =
    endpoint.post
      .summary("Обновление ответа на Stackoverflow")
      .in("updateAnswer")
      .in(jsonBody[UpdateQuestion])
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))
}
