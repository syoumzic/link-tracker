package tbank.academy.domain.model

import tbank.academy.domain.model.Config.BotConfig

case class Config(bot: BotConfig)

object Config {
  case class BotConfig(token: String)
}
