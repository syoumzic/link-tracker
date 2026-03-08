package tbank.academy.config

import cats.effect.Sync
import cats.implicits._
import com.comcast.ip4s.{Host, Port}
import pureconfig.error.CannotConvert
import pureconfig.generic.auto._
import pureconfig.{ConfigReader, ConfigSource}
import sttp.model.Uri
import tbank.academy.config.AppConfig.ServerConfig._
import tbank.academy.config.AppConfig.BotConfig._

import scala.concurrent.duration.{DurationLong, FiniteDuration}

case class AppConfig(
    server: AppConfig.ServerConfig,
    crawlers: AppConfig.Crawlers,
    bot: AppConfig.BotConfig
)

object AppConfig {
  def load[F[_]](implicit S: Sync[F]): F[AppConfig] =
    S.delay(ConfigSource.default.loadOrThrow[AppConfig])

  case class ServerConfig(host: Host, port: Port)

  object ServerConfig {
    implicit val HostConfigReader: ConfigReader[Host] = ConfigReader.fromString(rawHost =>
      Host.fromString(rawHost).toRight(CannotConvert(rawHost, "com.comcast.ip4s.Host", "неверно указан хост"))
    )

    implicit val PostConfigReader: ConfigReader[Port] = ConfigReader[Int].emap(rawPort =>
      Port.fromInt(rawPort).toRight(CannotConvert(rawPort.toString, "com.comcast.ip4s.Port", "неверно указан хост"))
    )
  }

  case class Crawlers(github: Crawlers.GithubConfig, stackoverflow: Crawlers.StackoverflowConfig)

  object Crawlers {
    case class GithubConfig(timeout: FiniteDuration)

    case class StackoverflowConfig(timeout: FiniteDuration)

    implicit val finiteDurationConfigReader: ConfigReader[FiniteDuration] = ConfigReader[Long].map(_.minutes)
  }

  case class BotConfig(uri: Uri)

  object BotConfig {
    implicit val uriConfigReader: ConfigReader[Uri] = ConfigReader[String].emap(rawUri =>
      Uri.parse(rawUri).leftMap(message => CannotConvert(rawUri, "sttp.model.Uri", message))
    )
  }
}
