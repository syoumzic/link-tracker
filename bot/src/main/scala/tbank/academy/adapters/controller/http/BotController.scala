package tbank.academy.adapters.controller.http

import alleycats.std.all._
import cats.effect.Async
import cats.implicits._
import sttp.tapir.server.ServerEndpoint
import tbank.academy.adapters.controller.http.endpoints.BotEndpoints
import tbank.academy.domain.telegram.TgService
import tbank.academy.http.Controller

trait BotController[F[_]] extends Controller[F] {
  def updatesPost: ServerEndpoint[Any, F]
  def updatePullRequest: ServerEndpoint[Any, F]
  def updateIssue: ServerEndpoint[Any, F]
  def updateComment: ServerEndpoint[Any, F]
  def updateAnswer: ServerEndpoint[Any, F]
}

object BotController {
  def make[F[_]](tgService: TgService[F])(implicit async: Async[F]): BotController[F] =
    new BotController[F] {
      override def updatesPost: ServerEndpoint[Any, F] =
        BotEndpoints.updatesPost
          .serverLogicSuccess { _ => async.unit }

      override def updatePullRequest: ServerEndpoint[Any, F] =
        BotEndpoints.updatePullRequest
          .serverLogicSuccess { pr =>
            pr.chatIds.traverse_(tgService.updatePullRequest(_, pr.url, pr.title, pr.username, pr.uptime))
          }

      override def updateIssue: ServerEndpoint[Any, F] =
        BotEndpoints.updateIssue
          .serverLogicSuccess { is =>
            is.chatIds.traverse_(tgService.updateIssue(_, is.url, is.title, is.username, is.uptime, is.description))
          }

      override def updateComment: ServerEndpoint[Any, F] =
        BotEndpoints.updateComment
          .serverLogicSuccess { co =>
            co.chatIds.traverse_(tgService.updateComment(_, co.question, co.username, co.uptime, co.description))
          }

      override def updateAnswer: ServerEndpoint[Any, F] =
        BotEndpoints.updateAnswer
          .serverLogicSuccess { an =>
            an.chatIds.traverse_(tgService.updateAnswer(_, an.question, an.username, an.uptime, an.description))
          }

      override val endpoints: List[ServerEndpoint[Any, F]] =
        List(
          updatesPost,
          updatePullRequest,
          updateIssue,
          updateComment,
          updateAnswer
        )
    }
}
