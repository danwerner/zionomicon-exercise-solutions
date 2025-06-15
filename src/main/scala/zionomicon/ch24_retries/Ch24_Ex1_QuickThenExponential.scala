package zionomicon.ch24_retries

import zio.*

/**
 * Create a schedule that first attempts 3 quick retries with a delay of 500 milliseconds.
 * If those fail, switch to exponential backoff for 5 more attempts, starting at 2 seconds
 * and doubling each time.
 */
object Ch24_Ex1_QuickThenExponential:

  val quickThenExpSchedule =
    val quick = Schedule.fixed(500.millis) && Schedule.recurs(3)
    val exp = Schedule.exponential(2.seconds, 2.0) && Schedule.recurs(5)
    quick `andThen` exp
