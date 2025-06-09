package zionomicon.ch11_queue

import zio.*
import zio.stream.*

/**
 * Implement load balancer that distributes work across multiple worker queues using
 * a round-robin strategy.
 */
object Ch11_Ex1_LoadBalancer:

  class LoadBalancer[A] private (
    val workerCount: Int,
    process: A => Task[A],
    currentQueueIndex: Ref[Int],
    queues: Chunk[Queue[A]]
  ):
    def submit(work: A): Task[Unit] =
      for
        currentIndex <- currentQueueIndex.getAndUpdate(i => (i + 1) % workerCount)
        queue = queues(currentIndex)
        _ <- queue.offer(work)
      yield ()

    def shutdown: Task[Unit] =
      queues.mapZIODiscard(_.shutdown)

  object LoadBalancer:
    def make[A](workerCount: Int, process: A => Task[A]): UIO[LoadBalancer[A]] =
      for
        currentQueueIndex <- Ref.make(0)
        queues <- Chunk.fromIterable(0 until workerCount)
          .mapZIO(_ => Queue.unbounded[A])
        _ <- queues.mapZIO(q => ZStream.fromQueue(q).mapZIO(process).runDrain.forkDaemon)
      yield LoadBalancer(workerCount, process, currentQueueIndex, queues)
