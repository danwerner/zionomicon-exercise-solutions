package zionomicon.ch21_stm

import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.*
import zionomicon.ch21_stm.Ch21_Ex2_CountDownLatch.CountDownLatch

object Ch21_Ex2_CountDownLatchSpec extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Chapter 21: STM: Composing Atomicity, Ex 2: CountDownLatch")(
      test("await should suspend the fiber until the count reaches zero") {
        val props = for
          counter <- Gen.int(2, 10)
          decThenNotDone <- Gen.int(1, counter - 1)
          decThenDone = counter - decThenNotDone
          decAfterDone = 0
        yield (counter, decThenNotDone, decThenDone, decAfterDone)

        checkCountDownLatch(props, (false, false, true, true))
      } @@ samples(2),

      test("await should allow counting down even after it has reached zero") {
        val props = for
          counter = 1
          decThenNotDone = 0
          decThenDone = 1
          decAfterDone <- Gen.int(1, 10)
        yield (counter, decThenNotDone, decThenDone, decAfterDone)

        checkCountDownLatch(props, (false, false, true, true))
      } @@ samples(1),

      test("await should never suspend if counter starts out at zero") {
        val props = Gen.const:
          val counter = 0
          val decThenNotDone = 0
          val decThenDone = 0
          val decAfterDone = 0
          (counter, decThenNotDone, decThenDone, decAfterDone)

        checkCountDownLatch(props, (true, true, true, true))
      } @@ samples(1)

    ) @@ withLiveClock @@ shrinks(0) @@ timeout(5.seconds) @@ sequential

  private def checkCountDownLatch(
    props: Gen[Any, (Int, Int, Int, Int)],
    expected: (Boolean, Boolean, Boolean, Boolean)
  ) =

    def awaitLatch(latch: CountDownLatch, doneRef: Ref[Boolean]) =
      latch.await *> doneRef.set(true)

    check(props) { case (counter, decThenNotDone, decThenDone, decAfterDone) =>
      for {
        doneRef <- Ref.make(false)
        latch <- CountDownLatch.make(counter)
        fiber <- awaitLatch(latch, doneRef).fork

        // 1. No count-down, await should block
        _ <- ZIO.sleep(100.millis)
        isDone1 <- doneRef.get

        // 2. Not counted down enough, await should block
        _ <- latch.countDown.repeatExactlyN(decThenNotDone)
        _ <- ZIO.sleep(100.millis)
        isDone2 <- doneRef.get

        // 3. Counted down enough, await should NOT block anymore
        _ <- latch.countDown.repeatExactlyN(decThenDone)
        _ <- ZIO.sleep(100.millis)
        isDone3 <- doneRef.get

        // 4. Counted down even further than possible, await should still not block anymore
        _ <- latch.countDown.repeatExactlyN(decAfterDone)
        _ <- ZIO.sleep(100.millis)
        isDone4 <- doneRef.get

        _ <- fiber.join
      } yield assert((isDone1, isDone2, isDone3, isDone4))(equalTo(expected))
    }

  extension [R, E, A](zio: ZIO[R, E, A])
    /**
     * Executes effect `zio` exactly n times. If n <= 0, does not execute `zio` at all and
     * immediately succeeds with `None`.
     *
     * This differs from the behavior of the standard [[ZIO.repeatN]] which repeats `zio`
     * an ''additional'' n times after the initial run.
     */
    private def repeatExactlyN(n: Int): ZIO[R, E, Option[A]] =
      ZIO.when(n > 0):
        zio.repeatN(n - 1)
