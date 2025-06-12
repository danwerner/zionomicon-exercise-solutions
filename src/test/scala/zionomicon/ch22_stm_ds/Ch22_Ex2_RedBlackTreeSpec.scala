package zionomicon.ch22_stm_ds

import zio.test.*
import zio.*
import zionomicon.ch22_stm_ds.Ch22_Ex2_RedBlackTree.RedBlackTree

object Ch22_Ex2_RedBlackTreeSpec extends ZIOSpecDefault {
  def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Chapter 22: STM: STM Data Structures, Ex 2: RedBlackTree")(
      test("empty tree should have size 0") {
        for {
          tree  <- RedBlackTree.make[Int, String]
          size <- tree.size.commit
        } yield assertTrue(size == 0)
      },

      test("get on empty returns None") {
        for {
          tree  <- RedBlackTree.make[Int, String]
          res  <- tree.get(42).commit
        } yield assertTrue(res.isEmpty)
      },

      test("put and get one element") {
        for {
          tree   <- RedBlackTree.make[Int, String]
          _     <- tree.put(1, "one").commit
          value <- tree.get(1).commit
        } yield assertTrue(value.contains("one"))
      },

      test("put overwrites existing value") {
        for {
          tree   <- RedBlackTree.make[Int, String]
          _     <- tree.put(1, "first").commit
          _     <- tree.put(1, "second").commit
          value <- tree.get(1).commit
        } yield assertTrue(value.contains("second"))
      },

      test("size reflects number of unique keys") {
        for {
          tree   <- RedBlackTree.make[Int, String]
          _     <- tree.put(1, "one").commit
          _     <- tree.put(2, "two").commit
          _     <- tree.put(1, "uno").commit // overwrite
          s     <- tree.size.commit
        } yield assertTrue(s == 2)
      },

      test("iterator yields sorted key/value pairs") {
        val pairs = List(5 -> "five", 3 -> "three", 8 -> "eight", 1 -> "one", 4 -> "four")
        val sorted = pairs.sortBy(_._1)

        for {
          shuffled <- Random.shuffle(pairs)
          tree     <- RedBlackTree.make[Int, String]
          _     <- ZIO.foreachDiscard(shuffled)(tree.put(_, _).commit)
          iter  <- tree.iterator.commit
        } yield assertTrue(iter.toList == sorted)
      },

      test("interleaved operations maintain correctness") {
        for {
          tree   <- RedBlackTree.make[Int, Int]
          _     <- tree.put(10, 100).commit
          _     <- tree.put(20, 200).commit
          _     <- tree.put(15, 150).commit
          a     <- tree.get(10).commit
          b     <- tree.get(15).commit
          _     <- tree.put(10, 110).commit
          c     <- tree.get(10).commit
          size1 <- tree.size.commit
          iter  <- tree.iterator.commit
        } yield assertTrue(
          a.contains(100) && b.contains(150) && c.contains(110) &&
            size1 == 3 && iter.toList == List(10 -> 110, 15 -> 150, 20 -> 200)
        )
      }
    )
}
