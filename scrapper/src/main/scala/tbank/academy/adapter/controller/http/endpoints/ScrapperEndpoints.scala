package tbank.academy.adapter.controller.http.endpoints

import io.circe.generic.auto._
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import tbank.academy.domain.model.TgChat
import tbank.academy.domain.model.http._

object ScrapperEndpoints {

  private val tgChatIdHeader: EndpointIO.Header[Long] =
    header[TgChat.Id]("Tg-Chat-Id").description("Идентификатор Telegram чата")

  private val tgChatRoute: EndpointInput[TgChat.Id] = "tg-chat" / path[TgChat.Id]("id")

  val tgChatPost: PublicEndpoint[TgChat.Id, (StatusCode, ApiErrorResponse), Unit, Any] =
    endpoint.post
      .summary("Зарегистрировать чат")
      .in(tgChatRoute)
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val tgChatDelete: PublicEndpoint[TgChat.Id, (StatusCode, ApiErrorResponse), Unit, Any] =
    endpoint.delete
      .summary("Удалить чат")
      .in(tgChatRoute)
      .out(statusCode(StatusCode.Ok))
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val linksGet
      : PublicEndpoint[(TgChat.Id, GetListLinksRequest), (StatusCode, ApiErrorResponse), ListLinksResponse, Any] =
    endpoint.get
      .summary("Получить все отслеживаемые ссылки")
      .in("links")
      .in(tgChatIdHeader)
      .in(jsonBody[GetListLinksRequest])
      .out(jsonBody[ListLinksResponse])
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val linksPost: PublicEndpoint[(TgChat.Id, AddLinkRequest), (StatusCode, ApiErrorResponse), LinkResponse, Any] =
    endpoint.post
      .summary("Добавить отслеживание ссылки")
      .in("links")
      .in(tgChatIdHeader)
      .in(jsonBody[AddLinkRequest])
      .out(jsonBody[LinkResponse])
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))

  val linksDelete: PublicEndpoint[(TgChat.Id, RemoveLinkRequest), (StatusCode, ApiErrorResponse), LinkResponse, Any] =
    endpoint.delete
      .summary("Убрать отслеживание ссылки")
      .in("links")
      .in(tgChatIdHeader)
      .in(jsonBody[RemoveLinkRequest])
      .out(jsonBody[LinkResponse])
      .errorOut(statusCode.and(jsonBody[ApiErrorResponse]))
}
