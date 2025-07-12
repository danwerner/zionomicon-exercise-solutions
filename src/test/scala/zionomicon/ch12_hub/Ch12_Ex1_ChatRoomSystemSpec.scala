package zionomicon.ch12_hub

import java.nio.file.Files
import scala.io.Source
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.TestAspect.{timeout, withLiveClock}
import zio.durationInt
import zionomicon.ch12_hub.Ch12_Ex1_ChatRoomSystem.*
import zionomicon.ch12_hub.Ch12_Ex1_ChatRoomSystem.ChatEvent.*

import scala.util.Using

object Ch12_Ex1_ChatRoomSystemSpec extends ZIOSpecDefault:

  private val user1 = Username("alice")
  private val user2 = Username("bob")

  def spec: Spec[TestEnvironment & Scope, Any] =
    suite("Chapter 12: Hub, Ex 1: ChatRoom system")(
      test("broadcasts join, message, and leave events to existing sessions and logs them") {
        ZIO.scoped {
          for
            // Create a temp log file
            logFile <- ZIO.attempt(Files.createTempFile("chat", ".log").toString)
            // Build the chat room with log file
            room <- ChatRoom.make(logFile)

            // User1 joins and subscribes
            s1 <- room.join(user1)
            q1 <- s1.receiveMessages

            // User2 joins, q1 should see the Join event
            s2 <- room.join(user2)
            eJoin <- q1.take.timeoutFail("user1 did not receive join event")(5.seconds)

            // User2 subscribes now
            q2 <- s2.receiveMessages

            // User1 sends messages
            _ <- s1.sendMessage("hello everyone")
            _ <- s1.sendMessage("how are you doing?")
            eMsgs1 <- q1.takeAll
            eMsgs2 <- q2.takeAll

            // User2 sends a message
            _ <- s2.sendMessage("fine, thanks!")
            eMsg3 <- q1.take.timeoutFail("user1 did not receive 'fine' message")(5.seconds)
            eMsg4 <- q2.take.timeoutFail("user2 did not receive 'fine' message")(5.seconds)

            // User2 leaves, q1 should see the Leave event
            _ <- s2.leave
            eLeave1 <- q1.take.timeoutFail("user1 did not receive leave event")(5.seconds)
            eLeaves2Exit <- q2.take.exit.timeoutFail("user2 queue hangs")(5.seconds)

            // User1 sends a lone message
            _ <- s1.sendMessage("oops, i scared them away")
            eMsg5 <- q1.take.timeoutFail("user1 did not receive 'oops' message")(5.seconds)

            // Give the logging fiber time to write
            _ <- ZIO.sleep(50.millis)
            logs <- ZIO.attempt:
              Using.resource(Source.fromFile(logFile)): logSource =>
                logSource.getLines().toList
          yield assert(eJoin)(equalTo(Join(user2))) &&
            assert(eMsgs1)(equalTo(Chunk(Message(user1, "hello everyone"), Message(user1, "how are you doing?")))) &&
            assert(eMsgs2)(equalTo(Chunk(Message(user1, "hello everyone"), Message(user1, "how are you doing?")))) &&
            assert(eMsg3)(equalTo(Message(user2, "fine, thanks!"))) &&
            assert(eMsg4)(equalTo(Message(user2, "fine, thanks!"))) &&
            assert(eMsg5)(equalTo(Message(user1, "oops, i scared them away"))) &&
            assert(eLeave1)(equalTo(Leave(user2))) &&
            assert(eLeaves2Exit)(isInterrupted) &&
            assert(logs)(equalTo(List(
              "* alice has joined",
              "* bob has joined",
              "alice: hello everyone",
              "alice: how are you doing?",
              "bob: fine, thanks!",
              "* bob has left",
              "alice: oops, i scared them away"
            )))
        }
      }
    ) @@ timeout(30.seconds) @@ withLiveClock
