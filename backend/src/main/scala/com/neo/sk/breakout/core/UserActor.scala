package com.neo.sk.breakout.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.Flow
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import com.neo.sk.medusa.snake.{Protocol, ProtocolFB}
import org.slf4j.LoggerFactory
import com.neo.sk.medusa.snake.ProtocolFB._
import java.awt.event.KeyEvent
import java.io.File

import com.neo.sk.breakout.Boot.{roomManager, userManager}
import com.neo.sk.medusa.protocol.RecordApiProtocol

import scala.concurrent.duration._
import scala.concurrent.duration.FiniteDuration
import io.circe.Decoder
import org.seekloud.byteobject.ByteObject.bytesDecode
import org.seekloud.byteobject.MiddleBufferInJvm

import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success}

object UserActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  //	private var counter = 0

  private final val InitTime = Some(5.minutes)

  private final val UserLeftTime = 10.minutes

  private final case object BehaviorChangeKey

  sealed trait Command

  final case object CompleteMessage extends Command

  final case class FailureMessage(ex: Throwable) extends Command

  case class UserFrontActor(actor: ActorRef[WsMsgSource]) extends Command

  case class UserWatchFrontActor(actor: ActorRef[WsMsgSource]) extends Command

  case class StartGame(playerId: String, playerName: String, roomId: Long, isNewUser: Boolean = true) extends Command

  case class JoinRoomSuccess(username: String, name: String, roomId: Long, roomActor: ActorRef[RoomActor.Command]) extends Command

  case class JoinRoomWatch(id: String, name:String, roomId: Long, roomActor: ActorRef[RoomActor.Command], isWait: Boolean) extends Command

  case class JoinRoomWait(roomId: Long, roomActor: ActorRef[RoomActor.Command]) extends Command


  case class JoinRoomFailure(roomId: Long, errorCode: Int, msg: String) extends Command

  private case class Key(playerId: String, keyCode: Int, px: Int, KeyCount: Int) extends Command

  private case class NetTest(id: String, createTime: Long) extends Command

  private case object RestartGame extends Command

  private case object UserLeft extends Command

  private case object StopReplay extends Command

  private case object UserDeadTimerKey extends Command

  private case class UnKnowAction(unknownMsg: UserAction) extends Command

  case class TimeOut(msg: String) extends Command


  case class DispatchMsg(msg: WsMsgSource) extends Command

  case class YouAreUnwatched(watcherId: String) extends Command

  case class ReplayGame(recordId: Long, watchPlayerId: String, frame: Long) extends Command

  case class ReplayData(data: Array[Byte]) extends Command

  case class GetRecordFrame(recordId: Long, sender: ActorRef[RecordApiProtocol.FrameInfo]) extends Command

  case class ReplayShot(shot: Array[Byte]) extends Command

  case class FrontLeft(frontActor: ActorRef[WsMsgSource]) extends Command

  case object ReplayOver extends Command

  case object KillSelf extends Command

  case class GetBallDetails(x: Double, y: Double, sx: Double, sy: Double) extends  Command

  case class JoinGame(id: String, name: String, roomId: String) extends Command

  case class CreateRoom(roomId: Long, password: String) extends Command

  case class NowModel(player1:String, player1Life:Boolean, player2:String, player2Life:Boolean,player1Score:Int) extends Command
  case class RecieveBarrage(msg :String) extends Command
  case class Back() extends  Command

  def create(playerId: String, playerName: String, roomId: Long): Behavior[Command] = {
    Behaviors.setup[Command] {
      ctx =>
        log.info(s"userActor ${ctx.self.path} start .....")
        implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
        Behaviors.withTimers[Command] {
          implicit timer =>
            switchBehavior(ctx, "init", init(playerId, playerName, roomId), InitTime, TimeOut("init"))
        }
    }
  }

  private def init(playerId: String, playerName: String, roomId: Long)
                  (implicit timer: TimerScheduler[Command], stashBuffer: StashBuffer[Command]): Behavior[Command] =
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {

          case UserFrontActor(frontActor) =>
//            ctx.watchWith(frontActor, FrontLeft(frontActor))
            switchBehavior(ctx, "idle", idle(playerId, playerName, roomId, frontActor))

          case x =>
            Behaviors.unhandled

        }
    }

  private def idle(playerId: String, playerName: String, roomId: Long, frontActor: ActorRef[ProtocolFB.WsMsgSource]
                  )(implicit timer: TimerScheduler[Command], stashBuffer: StashBuffer[Command]): Behavior[Command] = {
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {

          case t: JoinGame =>
            roomManager ! RoomManager.JoinGame(t.id, t.name, t.roomId.toLong, ctx.self)
            Behaviors.same

          case t: JoinRoomWait =>
            frontActor ! ProtocolFB.WaitOther("等待另一名玩家加入")
            Behaviors.same
          case t: JoinRoomFailure =>
            frontActor ! ProtocolFB.WaitOther("房间人数已满")
            Behaviors.same

          case t: JoinRoomWatch =>
            t.roomActor ! RoomActor.UserJoinGame(t.id, t.name, ctx.self)
            frontActor ! ProtocolFB.watchOther("玩家1开始游戏")
            switchBehavior(ctx, "watch", watch(t.id, t.name, t.roomId, frontActor, t.roomActor))

          case t: JoinRoomSuccess =>
            t.roomActor ! RoomActor.UserJoinGame(t.username, t.name, ctx.self)
            frontActor ! ProtocolFB.JoinGameSuccess(t.username, t.name, t.roomId)
            switchBehavior(ctx, "play", play(t.username, t.name, t.roomId, frontActor, t.roomActor))

          case UnKnowAction(unknownMsg) =>
            log.info(s"${ctx.self.path} receive an UnKnowAction when play:$unknownMsg")
            Behaviors.same

          //          case UserFrontActor(front) =>
          //            switchBehavior(ctx, "idle", idle(playerId, playerName, password, front))

//          case UserLeft =>
//            Behaviors.same

          case x =>
            log.error(s"${ctx.self.path} receive an unknown msg when idle:$x")
            Behaviors.unhandled
        }

    }
  }


  private def play(playerId: String, playerName: String, roomId: Long,
                   frontActor: ActorRef[ProtocolFB.WsMsgSource],
                   roomActor: ActorRef[RoomActor.Command]
                  )
                  (implicit timer: TimerScheduler[Command], stashBuffer: StashBuffer[Command]): Behavior[Command] = {
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {
          case t: Key =>
//            println(s"-------------playerId:${t.playerId}-- " + t)
            roomActor ! RoomActor.Key(t.playerId, t.keyCode, t.px, t.KeyCount)
            Behaviors.same
          case t:GetBallDetails =>
//            println("-----------:"+t)
            roomActor ! RoomActor.GetBallDetails(t.x,t.y,t.sx,t.sy)
            Behaviors.same
          case DispatchMsg(m) =>
//            println("p: "+ m)
            m match {
              //              case t: Protocol.SnakeDead => //  死亡时dead
              //                //如果死亡十分钟后无操作 则杀死userActor
              //                if (t.id == playerId) {
              ////                  timer.startSingleTimer(UserDeadTimerKey, FrontLeft(frontActor), UserLeftTime)
              //                  frontActor ! t
              //                  switchBehavior(ctx, "wait", wait(playerId, playerName, roomId, frontActor, roomActor))
              //                } else {
              //                  frontActor ! t
              //                  Behaviors.same
              //                }
              case t:PlayerList =>
//                println(ProtocolFB.PlayerList(t.playerList))
                frontActor ! ProtocolFB.PlayerList(t.player1,t.player2)
                Behaviors.same
//              case t:PaddleAction =>
//                frontActor ! ProtocolFB.GridDataForWatch(playerId,PaddleAction(t.playerId,t.keyCode,t.px,t.KeyCount))
//                Behavior.same
              case t:SyncBarrage =>
                frontActor ! ProtocolFB.SyncBarrage(t.msg)
                Behavior.same

              case t:SyncBallDetails =>
                Behaviors.same
              case t:OneLeft =>
                frontActor ! ProtocolFB.OneLeft()
                switchBehavior(ctx, "init", init(playerId, playerName, roomId), InitTime, TimeOut("init"))
              case x =>
                frontActor ! x
                Behaviors.same
            }

          case t:NowModel =>
            println("id: "+playerId+" now   play to watch ")
            //            if(t.player1==playerId && !t.player1Life){
              frontActor ! ProtocolFB.PlayToWatch()
              switchBehavior(ctx, "watch",watch(playerId, playerName, roomId, frontActor,roomActor))
//            }else {
//              switchBehavior(ctx, "play", play(playerId, playerName, roomId, frontActor, roomActor))
//            }


          case Back() =>
            roomManager ! RoomManager.UserLeftRoom(playerId,playerName,roomId)
            roomActor ! RoomActor.UserLeftRoomForS()
            switchBehavior(ctx, "init", init(playerId, playerName, roomId), InitTime, TimeOut("init"))


          //          case FrontLeft(frontActor) =>
//            println("1222222222222")
//            ctx.unwatch(frontActor)
//            //fon
//            roomManager ! RoomManager.UserLeftRoom(playerId,playerName,roomId)
//            roomActor ! RoomActor.UserLeftRoom(playerId,playerName,roomId)
//            //            userManager ! UserManager.UserGone(playerId)
//            Behavior.stopped

          case UnKnowAction(unknownMsg) =>
            log.debug(s"${ctx.self.path} receive an UnKnowAction when play:$unknownMsg")
            Behaviors.same

          case UserLeft =>
            println(playerId+ " play: 1222222222222")
            switchBehavior(ctx, "play", play(playerId, playerName, roomId, frontActor, roomActor),Some(3.seconds),TimeOut("when play left"))
            Behavior.same
          case TimeOut(msg) =>
            println(playerId+ " play:   "+msg)
            ctx.unwatch(frontActor)
            //fon
            roomManager ! RoomManager.UserLeftRoom(playerId,playerName,roomId)
            roomActor ! RoomActor.UserLeftRoom(playerId,playerName,roomId)
            //            userManager ! UserManager.UserGone(playerId)
            Behavior.stopped

          case x =>
            log.error(s"${ctx.self.path} receive an unknown msg when play:$x")
            Behaviors.unhandled
        }
    }
  }


  private def watch(playerId: String, playerName: String, roomId: Long,
                    frontActor: ActorRef[ProtocolFB.WsMsgSource],
                    roomActor: ActorRef[RoomActor.Command]
                   )
                   (implicit timer: TimerScheduler[Command], stashBuffer: StashBuffer[Command]): Behavior[Command] =
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {
          case DispatchMsg(m) =>
//            println("w: "+ m)
            m match {
              case t:PaddleAction =>
                frontActor ! ProtocolFB.GridDataForWatch(playerId,PaddleAction(t.playerName,t.keyCode,t.px,t.KeyCount))
                Behaviors.same
              case t:PlayerList =>
                frontActor ! ProtocolFB.PlayerList(t.player1,t.player2)
                Behaviors.same
              case t:SyncBallDetails =>
//                println(playerId+"   "+t)
                frontActor ! ProtocolFB.SyncBallDetails(t.x,t.y,t.sx,t.sy)
                Behaviors.same

              case t:SyncBarrage =>
                frontActor ! ProtocolFB.SyncBarrage(t.msg)
                Behavior.same
              case t:OneLeft =>
                frontActor ! ProtocolFB.OneLeft()
                switchBehavior(ctx, "init", init(playerId, playerName, roomId), InitTime, TimeOut("init"))
              case x =>
                frontActor ! x
                Behaviors.same

            }
          case t:GetBallDetails =>
            //            println("-----------:"+t)
            roomActor ! RoomActor.GetBallDetails(t.x,t.y,t.sx,t.sy)
            Behaviors.same
          case t:NowModel =>
            println("id: "+playerId+" now  watch to play")
//            if(t.player1==playerId && !t.player1Life){
//              switchBehavior(ctx, "watch",watch(playerId, playerName, roomId, frontActor,roomActor))
//            }else {
              frontActor ! ProtocolFB.Score(t.player1Score)
              frontActor !  ProtocolFB.JoinGameSuccess(playerId, playerName, roomId)
              switchBehavior(ctx, "play", play(playerId, playerName, roomId, frontActor, roomActor))
//            }


          case t:RecieveBarrage =>
            roomActor ! RoomActor.RecieveBarrage(t.msg)
            Behavior.same

//          case FrontLeft(frontActor) =>
//            println("1222222222222")
//
//            ctx.unwatch(frontActor)
//            //fon
//            roomManager ! RoomManager.UserLeftRoom(playerId,playerName,roomId)
//            roomActor ! RoomActor.UserLeftRoom(playerId,playerName,roomId)
////            userManager ! UserManager.UserGone(playerId)
//            Behavior.stopped

          case UserLeft =>
            println(playerId+ " play: 1222222222222")
            switchBehavior(ctx, "play", watch(playerId, playerName, roomId, frontActor, roomActor),Some(5.seconds),TimeOut("when watch left"))
            Behavior.same
          case TimeOut(msg) =>
            println(playerId+ " watch:   "+msg)
            ctx.unwatch(frontActor)
            //fon
            roomManager ! RoomManager.UserLeftRoom(playerId,playerName,roomId)
            roomActor ! RoomActor.UserLeftRoom(playerId,playerName,roomId)
            //            userManager ! UserManager.UserGone(playerId)
            Behavior.stopped
          case Back() =>
            roomManager ! RoomManager.UserLeftRoom(playerId,playerName,roomId)
            roomActor ! RoomActor.UserLeftRoomForS()
            switchBehavior(ctx, "init", init(playerId, playerName, roomId), InitTime, TimeOut("init"))

          case x =>
            log.error(s"${ctx.self.path} receive an unknown msg when watch:$x")
            Behaviors.unhandled
        }
    }



  private[this] def switchBehavior(ctx: ActorContext[Command],
                                   behaviorName: String,
                                   behavior: Behavior[Command],
                                   durationOpt: Option[FiniteDuration] = None,
                                   timeOut: TimeOut = TimeOut("busy time error"))
                                  (implicit timer: TimerScheduler[Command], stashBuffer: StashBuffer[Command]) = {
    log.info(s"${ctx.self.path} becomes $behaviorName behavior.")
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(duration => timer.startSingleTimer(BehaviorChangeKey, timeOut, duration))
    stashBuffer.unstashAll(ctx, behavior)
  }


  def flow(userActor: ActorRef[Command])(implicit decoder: Decoder[UserAction]): Flow[UserAction, WsMsgSource, Any] = {
    println("--------------------flow")
    val in =
      Flow[UserAction]
        .map {
          case ProtocolFB.Key(id, keyCode,px,keyCount) =>
//            if (keyCode == KeyEvent.VK_SPACE) {
//              RestartGame
//            } else {
              Key(id, keyCode,px,keyCount)
//            }
          case ProtocolFB.ChangeModel(player1,player1Life,player2,player2Life,player1Score) =>
            NowModel(player1,player1Life,player2,player2Life,player1Score)

          case ProtocolFB.JoinGame(id,name,roomId) =>
            JoinGame(id,name,roomId)
          case ProtocolFB.GetBallDetails(bx,by,bsx,bsy) =>
            GetBallDetails(bx,by,bsx,bsy)
          case ProtocolFB.SendBarrage(msg) =>
            RecieveBarrage(msg)
          case ProtocolFB.BackInit() =>
            Back()
          case x =>
            UnKnowAction(x)
        }
        .to(sink(userActor))

    val out =
      ActorSource.actorRef[WsMsgSource](
        completionMatcher = {
          case CompleteMsgServer =>
        },
        failureMatcher = {
          case FailMsgServer(ex) => ex
        },
        bufferSize = 64,
        overflowStrategy = OverflowStrategy.dropHead
      ).mapMaterializedValue { frontActor =>
        userActor ! UserFrontActor(frontActor)
      }

    Flow.fromSinkAndSource(in, out)
  }


  private def sink(actor: ActorRef[Command]) = ActorSink.actorRef[Command](
    ref = actor,
    onCompleteMessage = UserLeft,
    onFailureMessage = FailureMessage
  )




}
