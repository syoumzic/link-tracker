package tbank.academy.domain.model

import scala.util.control.NoStackTrace

trait DomainError extends Throwable with NoStackTrace
