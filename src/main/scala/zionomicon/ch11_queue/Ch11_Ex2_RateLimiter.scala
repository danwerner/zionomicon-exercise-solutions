package zionomicon.ch11_queue

import zio.*
import zio.stream.*

/**
 * Implement a rate limiter that limits the number of requests processed in a given time
 * frame. It takes the time interval and the maximum number of calls that are allowed
 * to be performed within the time interval.
 */
object Ch11_Ex2_RateLimiter:

  class RateLimiter private (val max: Int, val interval: Duration, queue: Queue[Unit]):

    def acquire: UIO[Unit] =
      // The fiber draining this stream will automatically be interrupted
      // once the queue is shut down.
      ZStream.fromQueue(queue)
        .rechunk(1)
        .throttleShape(max, interval, 0)(_.length)
        .runDrain
        .forkDaemon
        .unit

    def apply[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
      // Will backpressure when queue is full, effectively delaying execution
      // until there is room in the queue again.
      queue.offer(()) *> zio

    def shutdown: UIO[Unit] = queue.shutdown

    def awaitShutdown: UIO[Unit] = queue.awaitShutdown

    def isShutdown: UIO[Boolean] = queue.isShutdown

  object RateLimiter:
    def make(max: Int, interval: Duration): UIO[RateLimiter] =
      for
        queue <- Queue.bounded[Unit](max)
      yield RateLimiter(max, interval, queue)
