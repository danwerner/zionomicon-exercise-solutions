package zionomicon.ch21_stm

import zio.*
import zio.stm.*

/**
 * Implement a read-writer lock using STM. A read-writer lock allows multiple readers
 * to access a resource concurrently but requires exclusive access for writers.
 */
object Ch21_Ex3_ReadWriteLock:
  class ReadWriteLock private (writeLocked: TRef[Boolean]):
    def readWith[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
      zio

    def writeWith[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
      val acquireLock = (STM.whenSTM(writeLocked.get)(STM.retry) *> writeLocked.set(true)).commit
      val releaseLock = writeLocked.set(false).commit

      ZIO.acquireReleaseWith(acquireLock)(_ => releaseLock)(_ => zio)

  object ReadWriteLock:
    def make: UIO[ReadWriteLock] =
      TRef.makeCommit(false).map(ReadWriteLock(_))
