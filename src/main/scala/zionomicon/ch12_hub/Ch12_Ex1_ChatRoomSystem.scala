package zionomicon.ch12_hub

import cats.syntax.option.*
import io.github.iltotore.iron.RefinedType
import io.github.iltotore.iron.constraint.any.Pure
import zio.*
import zio.stream.*
import zionomicon.ch12_hub.Ch12_Ex1_ChatRoomSystem.ChatEvent.*

import java.io.{BufferedOutputStream, FileOutputStream, PrintStream}
import java.nio.charset.Charset

/**
 * Create a chatroom system using `Hub` where multiple users can join the chat so each
 * message sent is broadcast to all users. Each message sent by a user is received by all
 * other users. Users can leave the chat. There is also a process that logs all messages
 * sent to the chat room in a file.
 */
object Ch12_Ex1_ChatRoomSystem:

  type Username = Username.T
  object Username extends RefinedType[String, Pure]

  sealed trait ChatEvent

  object ChatEvent:
    final case class Join(username: Username) extends ChatEvent:
      override def toString: String = s"* $username has joined"

    final case class Leave(username: Username) extends ChatEvent:
      override def toString: String = s"* $username has left"

    final case class Message(fromUser: Username, message: String) extends ChatEvent:
      override def toString: String = s"$fromUser: $message"


  case class UserSession(
    username: Username,
    chatRoom: ChatRoom,
    eventQueue: Option[Dequeue[ChatEvent]]
  ):
    def sendMessage(message: String): UIO[Unit] =
      chatRoom.sendMessage(username, message)

    def receiveMessages: URIO[Scope, Dequeue[ChatEvent]] =
      chatRoom.receiveMessages(username)

    def leave: UIO[Unit] =
      chatRoom.shutdown(username)

  object UserSession:
    def make(username: Username, chatRoom: ChatRoom): UIO[UserSession] =
      ZIO.succeed(UserSession(username, chatRoom, None))


  class ChatRoom(
    users: Ref.Synchronized[Map[Username, UserSession]],
    messageHub: Hub[ChatEvent]
  ):
    def sendMessage(fromUser: Username, message: String): UIO[Unit] =
      messageHub.offer(Message(fromUser, message)).unit

    def join(username: Username): URIO[Scope, UserSession] =
      val joinUser = users.modifyZIO { map =>
        map.get(username) match
          case None =>
            UserSession.make(username, this).map: newUserSession =>
              (newUserSession, map + (username -> newUserSession))
          case Some(_) =>
            ZIO.dieMessage(s"User with name $username already in the chatroom")
      }
      joinUser <* messageHub.offer(Join(username))

    def receiveMessages(username: Username): URIO[Scope, Dequeue[ChatEvent]] =
      users.modifyZIO { map =>
        map.get(username) match
          case Some(userSession) => userSession.eventQueue match
            case Some(dequeue) =>
              ZIO.succeed((dequeue, map))
            case None =>
              for
                dequeue <- messageHub.subscribe
                userSession2 = userSession.copy(
                  eventQueue = dequeue.some
                )
              yield (dequeue, map + (username -> userSession2))
          case None =>
            ZIO.dieMessage("Should be impossible to call when the UserSession doesn't exist")
      }

    def shutdown(username: Username): UIO[Unit] =
      val removeUser = users.updateZIO { map =>
        map.get(username) match
          case Some(userSession) =>
            userSession.eventQueue
              .fold(ZIO.unit)(_.shutdown)
              .as(map - username)
          case None =>
            ZIO.dieMessage(s"Can't shutdown session for user $username, it doesn't exist")
      }
      removeUser <* messageHub.offer(Leave(username))

  object ChatRoom:
    def make(chatlogFile: String): URIO[Scope, ChatRoom] =
      for
        users      <- Ref.Synchronized.make(Map.empty[Username, UserSession])
        messageHub <- Hub.bounded[ChatEvent](1000)
        makeStream = ZIO.attempt {
          val fos = FileOutputStream(chatlogFile, true)
          PrintStream(BufferedOutputStream(fos), true, Charset.forName("UTF-8"))
        }
        outStream    <- ZIO.acquireRelease(makeStream)(os => ZIO.attempt(os.close()).orDie).orDie
        eventQueue   <- messageHub.subscribe
        chatlogFiber <- ZIO.blocking {
          ZStream.fromQueue(eventQueue).foreach { event =>
            ZIO.attempt(outStream.println(event.toString))
          }.orDie.forkScoped
        }
      yield
        ChatRoom(users, messageHub)
