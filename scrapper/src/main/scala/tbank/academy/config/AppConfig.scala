package tbank.academy.config

import cats.effect.Async
import cats.implicits._
import com.comcast.ip4s.{Host, Port}
import pureconfig.error.CannotConvert
import pureconfig.generic.auto._
import pureconfig.{ConfigReader, ConfigSource}
import sttp.model.Uri

import scala.concurrent.duration.{DurationLong, FiniteDuration}

case class AppConfig(
    server: ServerConfig,
    monitor: MonitorConfig,
    bot: BotConfig,
    doobie: DoobieConfig,
    `access-type`: String
)

case class DoobieConfig(driver: String, user: String, url: String, password: String)

case class ServerConfig(host: Host, port: Port)

case class MonitorConfig(timeout: FiniteDuration)

case class BotConfig(url: Uri)

object AppConfig {
  def load[F[_]](implicit async: Async[F]): F[AppConfig] =
    async.delay(ConfigSource.default.loadOrThrow[AppConfig])

  implicit val uriConfigReader: ConfigReader[Uri] = ConfigReader[String].emap(rawUri =>
    Uri.parse(rawUri).leftMap(message => CannotConvert(rawUri, "sttp.model.Uri", message))
  )

  implicit val finiteDurationConfigReader: ConfigReader[FiniteDuration] = ConfigReader[Long].map(_.minutes)

  implicit val HostConfigReader: ConfigReader[Host] = ConfigReader.fromString(rawHost =>
    Host.fromString(rawHost).toRight(CannotConvert(rawHost, "com.comcast.ip4s.Host", "неверно указан хост"))
  )

  implicit val PostConfigReader: ConfigReader[Port] = ConfigReader[Int].emap(rawPort =>
    Port.fromInt(rawPort).toRight(CannotConvert(rawPort.toString, "com.comcast.ip4s.Port", "неверно указан хост"))
  )
}
