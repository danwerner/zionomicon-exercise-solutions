package zionomicon.ch11_queue

import zio.*
import zio.test.*
import zionomicon.ch11_queue.Ch11_Ex2_RateLimiter.RateLimiter

object Ch11_Ex2_RateLimiterSpec extends ZIOSpecDefault:
  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Chapter 11: Queues, Ex 2: Rate Limiter")(
      test("apply should return the underlying effect’s result") {
        for
          rl <- RateLimiter.make(1, 1.second)
          _ <- rl.acquire
          result <- rl(ZIO.succeed("test"))
        yield assertTrue(result == "test")
      },

      test("under the rate limit, N calls in quick succession all complete instantly") {
        for
          rl <- RateLimiter.make(3, 1.second)
          _ <- rl.acquire
          results <- ZIO.foreachPar(1 to 3)(i => rl(ZIO.succeed(i)))
        yield assertTrue(results.toList == List(1, 2, 3))
      },

      // TODO: Add tests to ascertain rate limiting
    )

