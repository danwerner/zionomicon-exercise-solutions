package zionomicon.ch21_stm

import zio.*
import zio.stm.*

import java.lang.IllegalArgumentException as IAE

/**
 * Implement a simple countdown latch using ZIO STM’s TRef. A countdown latch
 * starts with a specified count (n). It provides two primary operations:
 *
 *   - countDown: Decrements the count by one but does nothing if it is already zero.
 *   - await: Suspends the calling fiber until the count reaches zero, allowing it to
 *     proceed only after all countdowns have been completed.
 *
 * Note: This exercise is for educational purposes to help you understand the basics of
 * STM. ZIO already provides a CountDownLatch implementation with more basic
 * concurrency primitives.
 */
object Ch21_Ex2_CountDownLatch:

  class CountDownLatch private (counter: TRef[Int]):
    def countDown: UIO[Unit] =
      STM.whenSTM(counter.get.map(_ > 0)) {
        counter.update(_ - 1)
      }.unit.commit

    def await: UIO[Unit] =
      STM.whenSTM(counter.get.map(_ > 0))(STM.retry).unit.commit

  object CountDownLatch:
    def make(initial: Int): IO[IllegalArgumentException, CountDownLatch] =
      (for
        _ <- STM.when(initial < 0)(STM.fail(IAE("initial < 0")))
        ref <- TRef.make(initial)
      yield CountDownLatch(ref))
        .commit
