package zionomicon.ch09_ref

import cats.syntax.option.*
import scala.collection.immutable.Queue
import zio.*

/**
 * Implement a bounded queue using Ref that has a maximum capacity that supports
 * the following interface.
 */
object Ch09_Ex2_BoundedQueue:

  class BoundedQueue[A] private (maxCapacity: Int, ref: Ref[Queue[A]]):

    /** Returns false if queue is full */
    def enqueue(a: A): UIO[Boolean] =
      ref.modify { queue =>
        if queue.length < maxCapacity then
          (true, queue.enqueue(a))
        else
          (false, queue)
      }

    /** Returns None if queue is empty */
    def dequeue: UIO[Option[A]] =
      ref.modify { queue =>
        queue.dequeueOption match
          case Some((a, rest)) =>
            (a.some, rest)
          case None =>
            (None, queue)
      }

    def size: UIO[Int] =
      ref.get.map(_.length)

    def capacity: UIO[Int] =
      ZIO.succeed(maxCapacity)

  object BoundedQueue:
    def make[A](maxCapacity: Int): UIO[BoundedQueue[A]] =
      Ref.make[Queue[A]](Queue.empty).map(BoundedQueue(maxCapacity, _))
