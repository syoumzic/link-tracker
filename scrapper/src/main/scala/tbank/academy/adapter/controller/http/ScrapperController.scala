package tbank.academy.adapter.controller.http

import cats.effect.Async
import cats.implicits._
import sttp.model.StatusCode
import sttp.tapir.server.ServerEndpoint
import tbank.academy.adapter.controller.http.endpoints.ScrapperEndpoints
import tbank.academy.adapter.http.controller.Controller
import tbank.academy.domain.model.Link
import tbank.academy.domain.model.Link.{LinkAlreadyExist, LinkNotFound, UnexpectedLink}
import tbank.academy.domain.model.http.{ApiErrorResponse, GetListLinksRequest, LinkResponse, ListLinksResponse}
import tbank.academy.domain.repository.ChatRepository
import tbank.academy.domain.repository.ChatRepository.{ChatAlreadyExist, ChatNotFound}
import tofu.logging.ServiceLogging.byUniversal
import tofu.logging.{Logging, LoggingCompanion}
import tofu.syntax.location.logging.LoggingInterpolator

trait ScrapperController[F[_]] extends Controller[F] {
  def tgChatPost: ServerEndpoint[Any, F]
  def tgChatDelete: ServerEndpoint[Any, F]
  def linksGet: ServerEndpoint[Any, F]
  def linksPost: ServerEndpoint[Any, F]
  def linksDelete: ServerEndpoint[Any, F]
}

object ScrapperController extends LoggingCompanion[ScrapperController] {
  def make[F[_]: Async: Logging.Make](
      chatService: ChatRepository[F]
  ): ScrapperController[F] =
    new ScrapperController[F] {
      override def tgChatPost: ServerEndpoint[Any, F] =
        ScrapperEndpoints.tgChatPost
          .serverLogic { id =>
            chatService
              .registerChat(id)
              .attempt
              .flatMap(mapError)
          }

      override val tgChatDelete: ServerEndpoint[Any, F] =
        ScrapperEndpoints.tgChatDelete
          .serverLogic { id =>
            chatService.deleteChat(id)
              .attempt
              .flatMap(mapError)
          }

      override val linksGet: ServerEndpoint[Any, F] =
        ScrapperEndpoints.linksGet
          .serverLogic { case (id, GetListLinksRequest(tag)) =>
            chatService
              .getLinks(id, tag)
              .map(ListLinksResponse.apply(id, _))
              .attempt
              .flatMap(mapError)
          }

      override val linksPost: ServerEndpoint[Any, F] =
        ScrapperEndpoints.linksPost
          .serverLogic { case (tgChatId, request) =>
            Link
              .generate(tgChatId, request.tags, request.link)
              .flatMap(chatService.addLink(tgChatId, _))
              .map(LinkResponse.apply(tgChatId, _))
              .attempt
              .flatMap(mapError)
          }

      override val linksDelete: ServerEndpoint[Any, F] =
        ScrapperEndpoints.linksDelete
          .serverLogic { case (tgChatId, request) =>
            chatService.deleteLink(tgChatId, request.link)
              .map(LinkResponse.apply(tgChatId, _))
              .attempt
              .flatMap(mapError)
          }

      override val endpoints: List[ServerEndpoint[Any, F]] =
        List(tgChatPost, tgChatDelete, linksGet, linksPost, linksDelete)

      private def errorResponse(
          description: String,
          code: StatusCode,
          exceptionName: String,
          exceptionMessage: String
      ): F[(StatusCode, ApiErrorResponse)] =
        (
          code,
          ApiErrorResponse(
            description = description,
            code = code.code.toString,
            exceptionName = exceptionName,
            exceptionMessage = exceptionMessage,
            stacktrace = Nil
          )
        ).pure[F]

      private def mapError[T](value: Either[Throwable, T]): F[Either[(StatusCode, ApiErrorResponse), T]] = value match {
        case Right(value) => Either.right[(StatusCode, ApiErrorResponse), T](value).pure[F]
        case Left(error)  => (error match {
            case ChatNotFound(id) => errorResponse(
                description = "Чат не найден",
                code = StatusCode.NotFound,
                exceptionName = "ChatNotFound",
                exceptionMessage = s"Чат с id = $id не найден"
              )
            case ChatAlreadyExist(id) => errorResponse(
                description = "Чат уже существует",
                code = StatusCode.Conflict,
                exceptionName = "ChatAlreadyExists",
                exceptionMessage = s"Чат с id = $id уже существует"
              )
            case LinkNotFound(url) => errorResponse(
                description = "Ссылка не найдена",
                code = StatusCode.NotFound,
                exceptionName = "LinkNotFound",
                exceptionMessage = s"Ссылка $url не найдена"
              )
            case LinkAlreadyExist(url) => errorResponse(
                description = "Ссылка уже существует",
                code = StatusCode.Conflict,
                exceptionName = "LinkAlreadyExists",
                exceptionMessage = s"Ссылка $url уже существует"
              )
            case UnexpectedLink(url) => errorResponse(
                description = "Недопустимая ссылка",
                code = StatusCode.BadRequest,
                exceptionName = "UnexpectedLink",
                exceptionMessage = s"Ссылка $url недопустима"
              )
            case unexpectedError => errorResponse(
                description = "Неизвестная ошибка",
                code = StatusCode.InternalServerError,
                exceptionName = "UnknownError",
                exceptionMessage = "Неизвестная ошибка"
              ).flatTap(_ => warnCause"Произошла внутреняя ошибка" (unexpectedError))
          }).map(Either.left[(StatusCode, ApiErrorResponse), T])
      }
    }
}
