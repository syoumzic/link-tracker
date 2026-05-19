package tbank.academy.domain.client

import cats.effect.Async
import cats.implicits._
import sttp.capabilities.fs2.Fs2Streams
import sttp.client4.tethysJson.asJson
import sttp.client4.{StreamBackend, basicRequest}
import sttp.model.Uri
import tethys.JsonReader
import tethys.jackson.jacksonTokenIteratorProducer

import scala.concurrent.duration.FiniteDuration

trait ApiClient[F[_]] {
  def execute[Out: JsonReader](url: String): F[Out]
}

object ApiClient {
  def make[F[_]](client: StreamBackend[F, Fs2Streams[F]], timeout: FiniteDuration)(implicit
      async: Async[F]
  ): ApiClient[F] = new ApiClient[F] {
    override def execute[Out: JsonReader](url: String): F[Out] =
      basicRequest
        .get(Uri.unsafeApply(url))
        .response(asJson[Out])
        .send(client)
        .map(_.body)
        .rethrow
        .flatTap(_ => async.sleep(timeout))
  }
}
