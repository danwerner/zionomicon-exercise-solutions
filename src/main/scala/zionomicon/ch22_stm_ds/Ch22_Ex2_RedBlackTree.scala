package zionomicon.ch22_stm_ds

import cats.syntax.option.*
import zio.{UIO, ZIO}
import zio.stm.*

import scala.annotation.tailrec

/**
 * Implement a red-black tree with ZIO STM that supports the following operations:
 *  - Insert a new element
 *  - Delete an element
 *  - Search for an element
 *  - Traverse the tree in-order
 *  - Balance the tree after each insertion or deletion
 *
 * Hint: Define a recursive RBNode data structure that represents the nodes of the
 * Red-Black Tree, and store the entire tree encapsulated in a single TRef.
 */
object Ch22_Ex2_RedBlackTree:

  sealed trait Color

  object Color:
    case object Red extends Color
    case object Black extends Color

  import Color.*

  case class RBNode[K, V](
    color: Color,
    key: K,
    value: V,
    left: Option[RBNode[K, V]],
    right: Option[RBNode[K, V]]
  )

  final class RedBlackTree[K: Ordering, V] private (root: TRef[Option[RBNode[K, V]]])(using kOrd: Ordering[K]):
    import kOrd.mkOrderingOps

    private type Node = RBNode[K, V]

    def get(key: K): USTM[Option[V]] =
      @tailrec
      def loop(n: Option[Node]): Option[V] = n match
          case Some(n) =>
            if key == n.key then n.value.some
            else if key < n.key then loop(n.left)
            else loop(n.right)
          case None => None

      root.get.map(loop)

    private def putFixup(tree: Node): Node = tree match
      case z @ RBNode(color = Black, left = Some(y @ RBNode(color = Red, left = Some(x @ RBNode(color = Red)), right = yr))) =>
        y.copy(
          left = Some(x.copy(color = Black)),
          right = Some(z.copy(left = yr))
        )
      case RBNode(Black, zk, zv, Some(RBNode(Red, xk, xv, xl, Some(RBNode(Red, yk, yv, b, c)))), d) =>
        RBNode(Red, yk, yv,
          Some(RBNode(Black, xk, xv, xl, b)),
          Some(RBNode(Black, zk, zv, c, d))
        )
      case RBNode(Black, xk, xv, a, Some(RBNode(Red, zk, zv, Some(RBNode(Red, yk, yv, b, c)), d))) =>
        RBNode(Red, yk, yv,
          Some(RBNode(Black, xk, xv, a, b)),
          Some(RBNode(Black, zk, zv, c, d))
        )
      case RBNode(Black, xk, xv, a, Some(RBNode(Red, yk, yv, b, Some(RBNode(Red, zk, zv, c, d))))) =>
        RBNode(Red, yk, yv,
          Some(RBNode(Black, xk, xv, a, b)),
          Some(RBNode(Black, zk, zv, c, d))
        )
      case _ => tree

    def put(key: K, value: V): USTM[Unit] =
      def ins(n: Option[Node]): Node = n match
        case Some(n) =>
          if key < n.key then
            putFixup(n.copy(left = ins(n.left).some))
          else if key > n.key then
            putFixup(n.copy(right = ins(n.right).some))
          else
            n.copy(value = value)

        case None =>
          RBNode(Red, key, value, None, None)

      root.update(n => ins(n).copy(color = Black).some)
      
    def iterator: USTM[Iterator[(K, V)]] =
      def loop(n: Option[Node]): LazyList[(K, V)] = n match
        case Some(n) => loop(n.left) #::: LazyList((n.key, n.value)) #::: loop(n.right)
        case None    => LazyList.empty

      root.get.map(loop(_).iterator)

    def size: USTM[Int] =
      iterator.map(_.size)

  object RedBlackTree:
    def make[K: Ordering, V]: UIO[RedBlackTree[K, V]] =
      TRef.makeCommit(None).map(RedBlackTree[K, V](_))
