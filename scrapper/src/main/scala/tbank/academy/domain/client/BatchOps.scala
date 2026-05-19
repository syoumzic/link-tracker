package tbank.academy.domain.client

import cats.effect.Async
import cats.implicits._
import fs2.Stream

object BatchOps {
  def batch[F[_]: Async, Out](batchSize: Int)(fetch: Int => F[List[Out]]): F[List[Out]] =
    Stream.iterate(1)(_ + batchSize)
      .evalMap(page => fetch(page))
      .takeThrough(_.size >= batchSize)
      .compile
      .toList
      .map(_.flatten)
}
