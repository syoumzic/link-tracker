package tbank.academy.domain.client

import cats.effect.Async
import cats.implicits._
import tbank.academy.adapter.client.http.Domain.{AnswerItem, CommentItem, QuestionItem, StackoverflowResponse}
import tbank.academy.config.AppConfig
import tbank.academy.domain.client.BatchOps.batch
import tofu.WithContext

trait StackoverflowBatchClient[F[_]] {
  def questions(baseUrl: String): F[List[QuestionItem]]
  def comments(baseUrl: String): F[List[CommentItem]]
  def answer(baseUrl: String): F[List[AnswerItem]]
}

object StackoverflowBatchClient {
  def make[F[_]: Async](client: ApiClient[F])(implicit
      context: WithContext[F, AppConfig]
  ): F[StackoverflowBatchClient[F]] =
    for {
      batchSize <- context.ask(_.batchClient.batchSize)
    } yield makeInternal(client)(batchSize)

  def makeInternal[F[_]: Async](client: ApiClient[F])(batchSize: Int): StackoverflowBatchClient[F] =
    new StackoverflowBatchClient[F] {
      override def questions(baseUrl: String): F[List[QuestionItem]] =
        batch(batchSize) { page =>
          client.execute[StackoverflowResponse[QuestionItem]](
            s"$baseUrl?page=$page&pagesize=$batchSize&site=stackoverflow.com, page, batchSize"
          ).map(_.items)
        }

      override def comments(baseUrl: String): F[List[CommentItem]] =
        batch(batchSize) { page =>
          client.execute[StackoverflowResponse[CommentItem]](
            s"$baseUrl/comments?page=$page&pagesize=$batchSize&site=stackoverflow.com, page, batchSize"
          ).map(_.items)
        }

      override def answer(baseUrl: String): F[List[AnswerItem]] =
        batch(batchSize) { page =>
          client.execute[StackoverflowResponse[AnswerItem]](
            s"$baseUrl/answers?page=$page&pagesize=$batchSize&site=stackoverflow.com, page, batchSize"
          ).map(_.items)
        }
    }
}
