package tbank.academy.config

import cats.effect.Sync
import cats.implicits.toBifunctorOps
import com.comcast.ip4s.{Host, Port}
import pureconfig.error.CannotConvert
import pureconfig.{ConfigReader, ConfigSource}
import tbank.academy.config.AppConfig.{BotConfig, ScrapperConfig, ServerConfig}
import pureconfig.generic.auto._
import sttp.model.Uri
import tbank.academy.config.AppConfig.ServerConfig._
import tbank.academy.config.AppConfig.ScrapperConfig._

case class AppConfig(bot: BotConfig, server: ServerConfig, scrapper: ScrapperConfig)

object AppConfig {
  case class BotConfig(token: String)

  def load[F[_]](implicit S: Sync[F]): F[AppConfig] = S.delay(ConfigSource.default.loadOrThrow[AppConfig])

  case class ServerConfig(host: Host, port: Port)

  object ServerConfig {
    implicit val HostConfigReader: ConfigReader[Host] = ConfigReader.fromString(rawHost =>
      Host.fromString(rawHost).toRight(CannotConvert(rawHost, "com.comcast.ip4s.Host", "неверно указан хост"))
    )

    implicit val PostConfigReader: ConfigReader[Port] = ConfigReader[Int].emap(rawPort =>
      Port.fromInt(rawPort).toRight(CannotConvert(rawPort.toString, "com.comcast.ip4s.Port", "неверно указан хост"))
    )
  }

  case class ScrapperConfig(url: Uri)

  object ScrapperConfig {
    implicit val uriConfigReader: ConfigReader[Uri] = ConfigReader[String].emap(rawUri =>
      Uri.parse(rawUri).leftMap(message => CannotConvert(rawUri, "sttp.model.Uri", message))
    )
  }
}
