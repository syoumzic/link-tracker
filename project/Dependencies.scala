import sbt.{compilerPlugin, *}

object Dependencies {
  val catsCore   = "org.typelevel" %% "cats-core"   % "2.13.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.7"

  val tethysVersion    = "0.29.8"
  val tethys           = "com.tethys-json" %% "tethys-core"       % tethysVersion
  val tethysJackson    = "com.tethys-json" %% "tethys-jackson213" % tethysVersion
  val tethysDerevation = "com.tethys-json" %% "tethys-derivation" % tethysVersion

  val tethysDerevoVersion     = "0.14.0"
  val tethysDerevo            = "tf.tofu" %% "derevo-core"   % tethysDerevoVersion
  val tethysDerevoIntegration = "tf.tofu" %% "derevo-tethys" % tethysDerevoVersion

  val tapirVersion = "1.13.12"
  val tapirHttp4s  = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion
  val tapirTethys  = "com.softwaremill.sttp.tapir" %% "tapir-json-tethys"   % tapirVersion

  val http4sVersion = "0.23.33"
  val http4sServer  = "org.http4s" %% "http4s-ember-server" % http4sVersion
  val http4sDsl     = "org.http4s" %% "http4s-dsl"          % http4sVersion

  val sttpVersion = "4.0.19"
  val sttpCore    = "com.softwaremill.sttp.client4" %% "core"           % sttpVersion
  val sttpHttp4s  = "com.softwaremill.sttp.client4" %% "http4s-backend" % sttpVersion
  val sttpTethys  = "com.softwaremill.sttp.client4" %% "tethys-json"    % sttpVersion

  val logback        = "ch.qos.logback"       % "logback-classic"          % "1.5.32"
  val logbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % "9.0"

  val pureConfigVersion = "0.17.10"
  val pureConfig        = "com.github.pureconfig" %% "pureconfig"      % pureConfigVersion
  val pureConfigCore    = "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion

  val tofuVersion           = "0.14.0"
  val tofuKernel            = "tf.tofu" %% "tofu-kernel"             % tofuVersion
  val tofuCoreCe3           = "tf.tofu" %% "tofu-core-ce3"           % tofuVersion
  val tofuLogging           = "tf.tofu" %% "tofu-logging"            % tofuVersion
  val tofuLoggingDerivation = "tf.tofu" %% "tofu-logging-derivation" % tofuVersion
  val tofuHigherKind        = "tf.tofu" %% "tofu-core-higher-kind"   % tofuVersion
  val tofuDerevoCore        = "tf.tofu" %% "derevo-core"             % tofuVersion
  val tofuDerevoCats        = "tf.tofu" %% "derevo-cats"             % tofuVersion
  val tofuDerevation        = "tf.tofu" %% "tofu-derivation"         % tofuVersion

  val canoe = "org.augustjune"     %% "canoe" % "0.6.0"
  val slick = "com.typesafe.slick" %% "slick" % "3.6.1"

  val doobieVersion  = "1.0.0-RC12"
  val doobie         = "org.tpolecat" %% "doobie-core"     % doobieVersion
  val doobiePostgres = "org.tpolecat" %% "doobie-postgres" % doobieVersion

  val psql    = "org.postgresql" % "postgresql"  % "42.7.10"
  val chimney = "io.scalaland"  %% "chimney"     % "1.9.0"
  val flyway  = "org.flywaydb"   % "flyway-core" % "9.22.0"

  val testContainersVersion   = "0.44.1"
  val testContainersScalatest = "com.dimafeng" %% "testcontainers-scala-scalatest"  % testContainersVersion
  val testContainersPostgress = "com.dimafeng" %% "testcontainers-scala-postgresql" % testContainersVersion

  val scalatest               = "org.scalatest"     %% "scalatest"            % "3.2.19"   % Test
  val scalatestPlusScalaCheck = "org.scalatestplus" %% "scalacheck-1-19"      % "3.2.19.0" % Test
  val scalacheckMagnolify     = "com.spotify"       %% "magnolify-scalacheck" % "0.9.4"    % Test

  val fs2Version = "3.13.0"
  val fs2        = "co.fs2" %% "fs2-core" % fs2Version
  val fs2IO      = "co.fs2" %% "fs2-io"   % fs2Version

  val scalamockVersion = "7.5.5"

  val scalamock     = "org.scalamock" %% "scalamock"             % scalamockVersion % Test
  val scalamockCats = "org.scalamock" %% "scalamock-cats-effect" % scalamockVersion % Test

  val allDeps: Seq[ModuleID] = Seq(
    catsCore,
    catsEffect,
    tethys,
    tethysJackson,
    tethysDerevo,
    tethysDerevation,
    tethysDerevoIntegration,
    tapirHttp4s,
    tapirTethys,
    http4sServer,
    http4sDsl,
    sttpCore,
    sttpHttp4s,
    sttpTethys,
    logback,
    pureConfig,
    pureConfigCore,
    tofuKernel,
    tofuLogging,
    tofuCoreCe3,
    tofuLoggingDerivation,
    tofuDerevoCats,
    tofuDerevation,
    canoe,
    scalatest,
    doobie,
    doobiePostgres,
    psql,
    chimney,
    flyway,
    testContainersScalatest,
    testContainersPostgress,
    slick,
    scalatestPlusScalaCheck,
    scalacheckMagnolify,
    fs2,
    fs2IO,
    scalamock,
    scalamockCats,
    compilerPlugin("org.typelevel" %% "kind-projector" % "0.13.4" cross CrossVersion.full),
  )
}
