package tbank.academy

import scala.util.control.NoStackTrace

trait DomainError extends Throwable with NoStackTrace
