import sbt.{compilerPlugin, *}

object Dependencies {
  // cats
  val catsCore   = "org.typelevel" %% "cats-core"   % "2.13.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.7"

  // tapir
  val tapirVersion = "1.13.12"

  val tapirHttp4s     = "com.softwaremill.sttp.tapir" %% "tapir-http4s-server"     % tapirVersion
  val tapirSwagger    = "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion
  val tapirSttpClient = "com.softwaremill.sttp.tapir" %% "tapir-sttp-client"       % tapirVersion
  val tapirNewtype    = "com.softwaremill.sttp.tapir" %% "tapir-newtype"           % tapirVersion
  val tapirCirce      = "com.softwaremill.sttp.tapir" %% "tapir-json-circe"        % tapirVersion

  // http4s
  val http4sVersion = "0.23.33"

  val http4sServer = "org.http4s" %% "http4s-ember-server" % http4sVersion
  val http4sDsl    = "org.http4s" %% "http4s-dsl"          % http4sVersion

  // sttp
  val sttpVersion = "4.0.19"

  val sttpCore   = "com.softwaremill.sttp.client4" %% "core"           % sttpVersion
  val sttpCats   = "com.softwaremill.sttp.client4" %% "cats"           % sttpVersion
  val sttpHttp4s = "com.softwaremill.sttp.client4" %% "http4s-backend" % sttpVersion
  val sttpCirce  = "com.softwaremill.sttp.client4" %% "circe"          % sttpVersion

  // logback
  val logback        = "ch.qos.logback"       % "logback-classic"          % "1.5.32"
  val logbackEncoder = "net.logstash.logback" % "logstash-logback-encoder" % "9.0"

  // pureconfig
  val pureConfigVersion = "0.17.10"
  val pureConfig        = "com.github.pureconfig" %% "pureconfig"      % pureConfigVersion
  val pureConfigCore    = "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion

  // neotype
  val newtypeVersion = "0.4.4"

  val newtype = "io.estatico" %% "newtype" % "0.4.4"

  val tofuVersion = "0.14.0"

  val tofuKernel            = "tf.tofu" %% "tofu-kernel"             % tofuVersion
  val tofuCoreCe3           = "tf.tofu" %% "tofu-core-ce3"           % tofuVersion
  val tofuLogging           = "tf.tofu" %% "tofu-logging"            % tofuVersion
  val tofuLoggingDerivation = "tf.tofu" %% "tofu-logging-derivation" % tofuVersion
  val tofuHigherKind        = "tf.tofu" %% "tofu-core-higher-kind"   % tofuVersion
  val tofuDerevoCore        = "tf.tofu" %% "derevo-core"             % tofuVersion
  val tofuDerevoCats        = "tf.tofu" %% "derevo-cats"             % tofuVersion
  val tofuDerevation        = "tf.tofu" %% "tofu-derivation"         % tofuVersion
  val tofuOpticsCore        = "tf.tofu" %% "tofu-optics-core"        % tofuVersion
  val tofuOpticsMacro       = "tf.tofu" %% "tofu-optics-macro"       % tofuVersion
  val tofuOpticsInterop     = "tf.tofu" %% "tofu-optics-interop"     % tofuVersion

  val canoe = "org.augustjune" %% "canoe" % "0.6.0"

  val scalatest               = "org.scalatest"     %% "scalatest"                     % "3.2.19"   % Test
  val scalatestCatsEffect     = "org.typelevel"     %% "cats-effect-testing-scalatest" % "1.7.0"    % Test
  val scalatestPlusScalaCheck = "org.scalatestplus" %% "scalacheck-1-19"               % "3.2.19.0" % Test
  val scalacheckMagnolify     = "com.spotify"       %% "magnolify-scalacheck"          % "0.9.4"    % Test

  val weaver     = "org.typelevel"  %% "weaver-cats"            % "0.12.0" % Test
  val mockServer = "org.mock-server" % "mockserver-client-java" % "5.15.0"

  val allDeps: Seq[ModuleID] = Seq(
    catsCore,
    catsEffect,
    tapirHttp4s,
    tapirSwagger,
    tapirSttpClient,
    tapirNewtype,
    tapirCirce,
    http4sServer,
    http4sDsl,
    sttpCore,
    sttpCats,
    sttpHttp4s,
    sttpCirce,
    logback,
    pureConfig,
    pureConfigCore,
    newtype,
    tofuKernel,
    tofuCoreCe3,
    tofuLogging,
    tofuLoggingDerivation,
    tofuDerevation,
    canoe,
    scalatest,
    scalatestCatsEffect,
    scalatestPlusScalaCheck,
    weaver,
    mockServer,
    compilerPlugin("org.typelevel" %% "kind-projector" % "0.13.4" cross CrossVersion.full),
  )
}
