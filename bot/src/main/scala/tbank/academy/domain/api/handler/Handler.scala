package tbank.academy.domain.api.handler

import canoe.api.Scenario

trait Handler[F[_]] {
  def scenario: Scenario[F, Unit]
}
