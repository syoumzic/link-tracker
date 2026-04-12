package tbank.academy.adapters.controller.http.endpoints

import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import tbank.academy.http._

object BotEndpoints {
  private val updates = "updates"

  val updatesPost: PublicEndpoint[LinkUpdate, (StatusCode, ApiErrorResponse), Unit, Any] =
    endpoint.post
      .summary("Отправить обновление")
      .in(updates)
      .in(jsonBody[LinkUpdate])
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))
}
