package tbank.academy.domain.model

import derevo.derive
import tofu.logging.derivation.loggable

@derive(loggable)
sealed trait Site

object Site {
  case object Github extends Site

  case object StackOverflow extends Site
}
