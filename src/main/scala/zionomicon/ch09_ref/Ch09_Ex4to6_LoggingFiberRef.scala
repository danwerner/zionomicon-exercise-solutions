package zionomicon.ch09_ref

import zio.*
import zionomicon.ch09_ref.Ch09_Ex4to6_LoggingFiberRef.FiberLogger.fmt

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 4. Implement a basic log renderer for the `FiberRef[Log]` we have defined through
 * the chapter. It should show the hierarchical structure of fiber logs using indentation:
 *
 *   - Each level of nesting should be indented by two spaces from the previous one.
 *   - The log entries for each fiber should be shown on separate lines
 *   - Child fiber logs should be shown under their parent fiber
 *
 * 5. Change the log model and use a more detailed one instead of just a String, so that
 * you can implement an advanced log renderer that adds timestamps and fiber IDs,
 * like the following output:
 *
 * {{{
 * [2024-01-01 10:00:01][fiber-1] Child foo
 *   [2024-01-01 10:00:02][fiber-2] Got 1
 *   [2024-01-01 10:00:03][fiber-2] Got 2
 * [2024-01-01 10:00:01][fiber-1] Child bar
 *   [2024-01-01 10:00:02][fiber-3] Got 3
 *   [2024-01-01 10:00:03][fiber-3] Got 4
 * }}}
 *
 * 6. Create a more advanced logging system that supports different log levels. It also
 * should support regional settings for log levels so that the user can change the log
 * level for a specific region of the application.
 */
object Ch09_Ex4to6_LoggingFiberRef:
  final case class LogEntry(
    ts: LocalDateTime,
    fiberId: String,
    message: String
  )

  final case class Tree[+A](head: A, tail: List[Tree[A]])

  type Log = Tree[Chunk[LogEntry]]

  val makeLoggingRef: ZIO[Scope, Nothing, FiberRef[Log]] =
    FiberRef.make[Log](
      Tree(Chunk.empty, Nil),
      fork = _ => Tree(Chunk.empty, Nil),
      join = (parent, child) => parent.copy(tail = child :: parent.tail)
    )

  class FiberLogger private (ref: FiberRef[Log]):

    def log(message: String): UIO[Unit] =
      for
        ts <- Clock.localDateTime
        fiberId <- ZIO.fiberId
        logEntry = LogEntry(ts, fiberId.id.toString, message)
        _ <- ref.update(log => log.copy(head = log.head :+ logEntry))
      yield ()

    def render: UIO[String] =
      ref.get.map(render)

    private def render(log: Log): String =
      val sb = StringBuilder()

      def formatLogEntry(le: LogEntry, level: Int): String =
        s"${" " * level * 2}[${fmt.format(le.ts)}][${le.fiberId}] ${le.message}\n"

      def recur(l: Log, level: Int): Unit =
        for (le <- l.head) {
          sb ++= formatLogEntry(le, level)
        }
        for (l <- l.tail) {
          recur(l, level + 1)
        }

      recur(log, 0)
      sb.toString

  object FiberLogger:
    def make: ZIO[Scope, Nothing, FiberLogger] =
      makeLoggingRef.map(FiberLogger.apply)

    private val fmt =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
