package tbank.academy.domain.client

import cats.effect.Async
import tbank.academy.adapter.client.http.Domain.GithubRequestItem
import tbank.academy.config.AppConfig
import tofu.WithContext
import cats.implicits._
import tbank.academy.domain.client.BatchOps.batch

trait GithubBatchClient[F[_]] {
  def events(baseUrl: String): F[List[GithubRequestItem]]
}

object GithubBatchClient {
  def make[F[_]: Async](client: ApiClient[F])(implicit context: WithContext[F, AppConfig]): F[GithubBatchClient[F]] =
    for {
      batchSize <- context.ask(_.batchClient.batchSize)
    } yield makeInternal(client)(batchSize)

  def makeInternal[F[_]: Async](client: ApiClient[F])(batchSize: Int): GithubBatchClient[F] = new GithubBatchClient[F] {
    def events(baseUrl: String): F[List[GithubRequestItem]] =
      batch(batchSize) { page =>
        client.execute[List[GithubRequestItem]](
          s"$baseUrl?page=$page&pagesize=$batchSize&site=stackoverflow.com"
        )
      }
  }
}
