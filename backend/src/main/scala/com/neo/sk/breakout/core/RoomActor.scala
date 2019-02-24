package com.neo.sk.breakout.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import com.neo.sk.breakout.breakBricks.GridOnServer
import com.neo.sk.medusa.snake.Breakout.PointFB
import com.neo.sk.medusa.snake.ProtocolFB
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

object RoomActor {
  private val log = LoggerFactory.getLogger(this.getClass)
  private val bound = PointFB(400, 400)
//  private var playerList = ListBuffer.empty[String]
  sealed trait Command
  private case object Sync extends Command
  case class UserJoinGame(playerId: String, playerName: String, userActor: ActorRef[UserActor.Command]) extends Command
  case class Key(playerId: String, keyCode: Int,px:Int,KeyCount:Int) extends Command
  case class GetBallDetails(x: Double, y: Double, sx: Double, sy: Double) extends  Command
  case class PlayerList(playerList: ListBuffer[Long]) extends  Command
  case class RecieveBarrage(msg :String) extends Command
  case class UserLeftRoom(playerName:String,password:String,roomId:Long) extends Command
  case class UserLeftRoomForS() extends Command

  private case object BeginSync extends Command

  private case object TimerKey4SyncBegin

  private case object TimerKey4SyncLoop

  private case object TimerKey4CloseRec

  def create(roomId: Long): Behavior[Command] = {
    Behaviors.setup[Command] {
      ctx =>
        log.info(s"roomActor ${ctx.self.path} start.....")
        implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
        Behaviors.withTimers[Command] {
          implicit timer =>
//            timer.startSingleTimer(TimerKey4SyncBegin, BeginSync, 1.seconds)
            val grid = new GridOnServer(bound, ctx.self)
            val userMap = mutable.HashMap.empty[String,(String,ActorRef[UserActor.Command])]
            val playerList = ListBuffer.empty[String]
            idle(roomId,userMap,grid,playerList)
        }
    }
  }
  private def idle(roomId:Long,userMap:mutable.HashMap[String,(String,ActorRef[UserActor.Command])],grid:GridOnServer,playerList:ListBuffer[String])
                  (implicit timer: TimerScheduler[RoomActor.Command]): Behavior[Command] = {
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {

          case t: UserJoinGame =>
            log.info(s"room $roomId got a new player: ${t.playerId}")
            timer.cancel(TimerKey4CloseRec)
            userMap.put(t.playerId,(t.playerName,t.userActor))
            grid.addUser(t.playerId.toString,t.playerName)
            playerList.append(t.playerId)
            if(playerList.length == 2){
              dispatch(userMap,ProtocolFB.PlayerList(playerList(0),playerList(1)))
            }
            idle(roomId,userMap,grid,playerList)


          case t:Key =>
            dispatch(userMap,ProtocolFB.PaddleAction(t.playerId,t.keyCode,t.px,t.KeyCount))
            Behaviors.same


          case t:GetBallDetails =>
//            println("--------: "+t)
            dispatch(userMap,ProtocolFB.SyncBallDetails(t.x,t.y,t.sx,t.sy))
            Behaviors.same


          case t:RecieveBarrage =>
            dispatch(userMap,ProtocolFB.SyncBarrage(t.msg))
            Behavior.same

          case t:UserLeftRoom =>
            userMap.remove(t.playerName)
            dispatch(userMap,ProtocolFB.OneLeft())
            userMap.clear()
            Behavior.same


          case t:UserLeftRoomForS =>
            userMap.clear()
            Behavior.stopped


          case BeginSync =>
            timer.startPeriodicTimer(TimerKey4SyncLoop, Sync, 100.millis)
            Behaviors.same
        }
    }
  }

  def dispatch(userMap: mutable.HashMap[String, (String,ActorRef[UserActor.Command])],
               toMsg: ProtocolFB.WsMsgSource) = {
    userMap.values.foreach { t => t._2 ! UserActor.DispatchMsg(toMsg) }
  }
}
