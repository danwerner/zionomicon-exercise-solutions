package zionomicon.ch11_queue

import zio.*
import zio.stream.*

/**
 * Implement a rate limiter that limits the number of requests processed in a given time
 * frame. It takes the time interval and the maximum number of calls that are allowed
 * to be performed within the time interval.
 */
object Ch11_Ex2_RateLimiter:

  class RateLimiter(val max: Int, val interval: Duration, queue: Queue[Unit]):

    def acquire: UIO[Unit] =
      ZStream.fromQueue(queue)
        .throttleShape(max, interval, 0)(_ => 1)
        .runDrain
        .forkDaemon
        .unit

    def apply[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
      ???

  object RateLimiter:
    def make(max: Int, interval: Duration): UIO[RateLimiter] =
      for
        queue <- Queue.dropping[Unit](max)
      yield RateLimiter(max, interval, queue)
