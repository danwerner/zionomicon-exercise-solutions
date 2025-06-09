import sbt.*

object Dependencies {

  object ZIO {
    val zioVersion = "2.1.19"

    val zio = "dev.zio" %% "zio" % zioVersion
  }

  object Testing {
    val zioTest = "dev.zio" %% "zio-test" % ZIO.zioVersion
  }

  val dependencies: List[ModuleID] = List(
    ZIO.zio
  )

  val testDependencies: List[ModuleID] = List(
    Testing.zioTest
  )

}
