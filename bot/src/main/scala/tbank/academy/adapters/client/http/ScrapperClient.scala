package tbank.academy.adapters.client.http

import cats.effect.Async
import cats.implicits._
import sttp.capabilities.fs2.Fs2Streams
import sttp.client4.{StreamBackend, basicRequest}
import sttp.model.Uri
import tbank.academy.config.AppConfig
import tbank.academy.domain.client.ScrapperClient
import tofu.WithContext
import sttp.client4.tethysJson.{asJson, asJsonEitherOrFail}
import tbank.academy.DomainError
import tbank.academy.http._
import tethys.jackson.{jacksonTokenIteratorProducer, jacksonTokenWriterProducer}
import tbank.academy.Predef._

object ScrapperClient {
  trait Error extends DomainError {
    val message: String
  }

  case class LinkAlreadyExist(override val message: String) extends Error
  case class ChatAlreadyExist(override val message: String) extends Error
  case class ChatNotFound(override val message: String)     extends Error
  case class UnexpectedLink(override val message: String)   extends Error
  case class Conflict(override val message: String)         extends Error
  case class BadRequest(override val message: String)       extends Error
  case class LinkNotFound(override val message: String)     extends Error

  def make[F[_]: Async](http: StreamBackend[F, Fs2Streams[F]])(
      implicit context: WithContext[F, AppConfig]
  ): ScrapperClient[F] = {
    new ScrapperClient[F] {
      override def linksPost(
          chatId: Long,
          linkUrl: String,
          tags: Set[String],
          filters: Set[String]
      ): F[LinkResponse] =
        url.flatMap { url =>
          basicRequest
            .post(url.addPath("links"))
            .header("Tg-Chat-Id", chatId.toString)
            .body(asJson(AddLinkRequest(linkUrl, tags, filters)))
            .response(asJsonEitherOrFail[ApiErrorResponse, LinkResponse])
            .send(http)
            .map {
              _.body.leftMap(recoverError)
            }
            .rethrow
        }

      override def tgChatPost(chatId: Long): F[Unit] =
        url.flatMap { url =>
          basicRequest
            .post(url.addPath("tg-chat").addParam("id", chatId.toString))
            .response(asJsonEitherOrFail[ApiErrorResponse, Unit])
            .send(http)
            .map {
              _.body.leftMap(recoverError)
            }
            .rethrow
        }

      override def tgChatDelete(chatId: Long): F[Unit] =
        url.flatMap { url =>
          basicRequest
            .delete(url.addPath("tg-chat").addParam("id", chatId.toString))
            .response(asJsonEitherOrFail[ApiErrorResponse, Unit])
            .send(http)
            .map {
              _.body.leftMap(recoverError)
            }
            .rethrow

        }

      override def linksGet(chatId: Long, tag: Option[String]): F[ListLinksResponse] =
        url.flatMap { url =>
          basicRequest
            .get(url.addPath("links"))
            .header("Tg-Chat-Id", chatId.toString)
            .response(asJsonEitherOrFail[ApiErrorResponse, ListLinksResponse])
            .body(asJson(GetListLinksRequest(tag)))
            .send(http)
            .map {
              _.body.leftMap(recoverError)
            }
            .rethrow
        }

      override def linksDelete(chatId: Long, link: String): F[LinkResponse] =
        url.flatMap { url =>
          basicRequest
            .delete(url.addPath("links"))
            .header("Tg-Chat-Id", chatId.toString)
            .body(asJson(RemoveLinkRequest(link)))
            .response(asJsonEitherOrFail[ApiErrorResponse, LinkResponse])
            .send(http)
            .map {
              _.body.leftMap(recoverError)
            }
            .rethrow
        }

      private def url: F[Uri] = context.ask(_.scrapper.url)

      private def recoverError(errorResponse: ApiErrorResponse): Error = errorResponse match {
        case ApiErrorResponse(description, code, exceptionName, exceptionMessage, stacktrace) =>
          exceptionName match {
            case "LinkAlreadyExist" => LinkAlreadyExist(description)
            case "LinkNotFound"     => LinkNotFound(description)
            case "ChatAlreadyExist" => ChatAlreadyExist(description)
            case "ChatNotFound"     => ChatNotFound(description)
            case "UnexpectedLink"   => UnexpectedLink(description)
            case "Conflict"         => Conflict(description)
          }
      }
    }
  }
}
