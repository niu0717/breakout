package com.neo.sk.breakout.core

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorAttributes, Supervision}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.slf4j.LoggerFactory
import com.neo.sk.medusa.snake.ProtocolFB._
import io.circe.generic.auto._

import scala.collection.mutable

object UserManager {
  private val log = LoggerFactory.getLogger(this.getClass)
  sealed trait Command
  final case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command
  final case class GetWebSocketFlow(playerId: String, playerName: String, roomId: Long, replyTo: ActorRef[Flow[Message, Message, Any]]) extends Command
  case class UserGone(playerId: String) extends Command



  val behaviors: Behavior[Command] = {
    log.info(s"UserManager start...")
    Behaviors.setup[Command] {
      _ =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            val allUser = mutable.HashMap.empty[String, ActorRef[UserActor.Command]]
            val userCreateRoom = mutable.HashMap.empty[String, Int]
            //    timer.startSingleTimer(Timer4MsgAdd, ClearMsgLength, 10000.milli)
            idle( userCreateRoom, allUser)
        }
    }
  }

  def idle(
           userCreateRoom: mutable.HashMap[String, Int],
           allUser: mutable.HashMap[String, ActorRef[UserActor.Command]])
          (implicit timer: TimerScheduler[Command]): Behavior[Command] =
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {
          case GetWebSocketFlow(playerId, playerName, roomId, replyTo) =>
            //此处的roomId是没有任何作用的
            val user = getUserActor(ctx, playerId, playerName,roomId)
//            allUser.put(playerId, user)
            replyTo ! getWebSocketFlow(user)
            Behaviors.same

          case ChildDead(name, childRef) =>
            log.info(s"UserActor $name is dead ")
            ctx.unwatch(childRef)
            Behaviors.same
        }
    }
  private def getUserActor(ctx: ActorContext[Command], playerId: String, playerName: String,roomId:Long): ActorRef[UserActor.Command] = {
    val childName = s"UserActor-"+playerId.hashCode
    ctx.child(childName).getOrElse {
      log.info(s"create user actor $childName")
      val actor = ctx.spawn(UserActor.create(playerId, playerName,roomId), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.upcast[UserActor.Command]
  }
  private def getWebSocketFlow(userActor: ActorRef[UserActor.Command]): Flow[Message, Message, Any] = {
    Flow[Message]
      .collect {
        case TextMessage.Strict(msg) =>
          log.debug(s"msg from webSocket: $msg")
          TextInfo("-1", msg)

        case BinaryMessage.Strict(bMsg) =>
          //decode process.

          val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
          val msg =
            bytesDecode[UserAction](buffer) match {
              case Right(v) => v
              case Left(e) =>
                println(s"decode error: ${e.message}")
                TextInfo("-1", "decode error")
            }
          msg
        // unpack incoming WS text messages...
        // This will lose (ignore) messages not received in one chunk (which is
        // unlikely because chat messages are small) but absolutely possible
        // FIXME: We need to handle TextMessage.Streamed as well.
      }
      .via(UserActor.flow(userActor)) // ... and route them through the chatFlow ...
      .map { //... pack outgoing messages into WS JSON messages ...
      case message: GameMessageFB =>
        val sendBuffer = new MiddleBufferInJvm(163840)
        val msg = ByteString(
          //encoded process
          message.fillMiddleBuffer(sendBuffer).result()
        )
        //msgLength += msg.length
        val a = BinaryMessage.Strict(msg)
        a

      case _ =>
        TextMessage.apply("")
    }.withAttributes(ActorAttributes.supervisionStrategy(decider)) // ... then log any processing errors on stdin
  }

  val decider: Supervision.Decider = {
    e: Throwable =>
      e.printStackTrace()
      println(s"WS stream failed with $e")
      Supervision.Resume
  }
}
