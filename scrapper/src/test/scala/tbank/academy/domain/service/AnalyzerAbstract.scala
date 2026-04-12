package tbank.academy.domain.service

import cats.effect.IO
import fs2.io.readClassLoaderResource
import fs2.text.utf8
import tethys.JsonReader
import tethys._
import tethys.jackson._

//scalafix:off Disable.Option.get
trait AnalyzerAbstract {
  def getJson[A: JsonReader](input: String): IO[A] =
    readClassLoaderResource[IO](
      name = s"$input.json",
      classLoader = getClass.getClassLoader
    )
      .through(utf8.decode)
      .map(a => a.jsonAs[A])
      .rethrow
      .compile
      .last
      .map(_.get)
}
//scalafix:on Disable.Option.get
