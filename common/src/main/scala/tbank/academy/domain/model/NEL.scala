package tbank.academy.domain.model

import cats.data.NonEmptyList

object NEL {
  type NEL[A] = NonEmptyList[A]
}
