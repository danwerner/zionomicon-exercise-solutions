package zionomicon.ch09_ref

import zio.*
import zionomicon.ch09_ref.Ch09_Ex4to6_LoggingFiberRef.FiberLogger

object Ch09_Ex4to6_LoggingFiberRefScript extends ZIOAppDefault:

  override def run: ZIO[ZIOAppArgs & Scope, Any, Any] =
    for {
      logger <- FiberLogger.make
      _ <-  logger.log("Got 0").as(0).randomDelay
      left = for {
        a <- logger.log("Got 1").as(1).randomDelay
        b <- logger.log("Got 2").as(2).randomDelay
      } yield a + b
      right = for {
        c <- logger.log("Got 3").as(3).randomDelay
        d <- logger.log("Got 4").as(4).randomDelay
        fiber3 <- logger.log("Got 5").as(5).randomDelay.fork
        e <- fiber3.join
      } yield c + d
      fiber1 <- left.randomDelay.fork
      fiber2 <- right.randomDelay.fork
      _ <- fiber1.join
      _ <- fiber2.join
      output <- logger.render
      _ <- Console.printLine(output)
    } yield ()

  extension [R, E, A](zio: ZIO[R, E, A])
    private def randomDelay =
      Random.nextIntBetween(0, 1000)
        .flatMap(delay => zio.delay(delay.millis))
