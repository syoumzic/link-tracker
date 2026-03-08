package tbank.academy.domain.telegram.handler

import canoe.api.Scenario

trait Handler[F[_]] {
  def scenario: Scenario[F, Unit]
}
