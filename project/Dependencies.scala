import sbt.*

object Dependencies {

  object Cats {
    val catsVersion = "2.13.0"

    val cats = "org.typelevel" %% "cats-core" % catsVersion
  }

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
    Cats.cats,
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
