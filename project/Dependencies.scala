import sbt.*

object Dependencies {

  object Iron {
    val ironVersion = "3.0.2-RC1"

    val iron     = "io.github.iltotore" %% "iron"     % ironVersion
  }

  object ZIO {
    val zioVersion = "2.1.19"

    val zio        = "dev.zio" %% "zio"         % zioVersion
    val zioStreams = "dev.zio" %% "zio-streams" % zioVersion
  }

  object Testing {
    val zioTest         = "dev.zio" %% "zio-test"          % ZIO.zioVersion
    val zioTestSbt      = "dev.zio" %% "zio-test-sbt"      % ZIO.zioVersion
    val zioTestMagnolia = "dev.zio" %% "zio-test-magnolia" % ZIO.zioVersion
  }

  val dependencies: List[ModuleID] = List(
    Iron.iron,
    ZIO.zio,
    ZIO.zioStreams
  )

  val testDependencies: List[ModuleID] = List(
    Testing.zioTest,
    Testing.zioTestSbt,
    Testing.zioTestMagnolia
  )

}
