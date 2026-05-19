package tbank.academy.adapter.controller.http.endpoints

import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.tethys.jsonBody
import tbank.academy.http._

object ScrapperEndpoints {

  private val tgChatIdHeader: EndpointIO.Header[Long] =
    header[Long]("Tg-Chat-Id").description("Идентификатор Telegram чата")

  private val tgChatRoute: EndpointInput[Long] = "tg-chat" / path[Long]("id")

  val tgChatPost: PublicEndpoint[Long, (StatusCode, ApiErrorResponse), Unit, Any] =
    endpoint.post
      .summary("Зарегистрировать чат")
      .in(tgChatRoute)
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val tgChatDelete: PublicEndpoint[Long, (StatusCode, ApiErrorResponse), Unit, Any] =
    endpoint.delete
      .summary("Удалить чат")
      .in(tgChatRoute)
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val linksGet
      : PublicEndpoint[(Long, GetListLinksRequest), (StatusCode, ApiErrorResponse), ListLinksResponse, Any] =
    endpoint.get
      .summary("Получить все отслеживаемые ссылки")
      .in("links")
      .in(tgChatIdHeader)
      .in(jsonBody[GetListLinksRequest])
      .out(jsonBody[ListLinksResponse])
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val linksPost: PublicEndpoint[(Long, AddLinkRequest), (StatusCode, ApiErrorResponse), LinkResponse, Any] =
    endpoint.post
      .summary("Добавить отслеживание ссылки")
      .in("links")
      .in(tgChatIdHeader)
      .in(jsonBody[AddLinkRequest])
      .out(jsonBody[LinkResponse])
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val linksDelete: PublicEndpoint[(Long, RemoveLinkRequest), (StatusCode, ApiErrorResponse), LinkResponse, Any] =
    endpoint.delete
      .summary("Убрать отслеживание ссылки")
      .in("links")
      .in(tgChatIdHeader)
      .in(jsonBody[RemoveLinkRequest])
      .out(jsonBody[LinkResponse])
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))
}
