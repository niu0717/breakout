package com.neo.sk.breakout.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import org.slf4j.LoggerFactory

import scala.collection.mutable

object RoomManager {
  private val log = LoggerFactory.getLogger(this.getClass)
  sealed trait Command
  final case class ChildDead[U](roomId: Long, childRef: ActorRef[U]) extends Command
  case class JoinGame(playerId: String, playerName: String, roomId: Long,  userActor: ActorRef[UserActor.Command]) extends Command
  case class UserLeftRoom(playerName:String,password:String,roomId:Long) extends Command
  val behaviors: Behavior[Command] = {
    log.info(s"RoomManager start...")
    Behaviors.setup[Command] {
      _ =>
        Behaviors.withTimers[Command] {
          implicit timer =>
            //roomId->userNum
            val roomNumMap = mutable.HashMap.empty[Long, (String,String)]
            val userRoomMap = mutable.HashMap.empty[String, (Long, String)]
            val waitUserMap = mutable.HashMap.empty[Long,(String,String,ActorRef[UserActor.Command])]
            idle(roomNumMap, userRoomMap,waitUserMap)
        }
    }
  }


  private def idle(roomInfoMap: mutable.HashMap[Long,  (String,String)], //房间-->[用户1，用户2]
                   userRoomMap: mutable.HashMap[String, (Long, String)],
                   waitUserMap:mutable.HashMap[Long,(String,String,ActorRef[UserActor.Command])])
                  (implicit timer: TimerScheduler[Command]) =
    Behaviors.receive[Command] {
      (ctx, msg) =>
        msg match {
          case JoinGame(playerId, playerName, roomId, userActor) =>
            if(roomInfoMap.contains(roomId)){   //存在的房间再进入一个人

              if(roomInfoMap(roomId)._1!="" && roomInfoMap(roomId)._2!="") {
                userActor ! UserActor.JoinRoomFailure(roomId,2,"房间人数已满")
              }else{
                if(roomInfoMap(roomId)._1!="" && roomInfoMap(roomId)._2==""){
                  roomInfoMap.update(roomId,(roomInfoMap(roomId)._1,playerId))
                }
                if(roomInfoMap(roomId)._1!="" && roomInfoMap(roomId)._2!=""){
                  userActor ! UserActor.JoinRoomWatch(playerId,playerName,roomId,getRoomActor(ctx,roomId),true)
                  waitUserMap(roomId)._3 ! UserActor.JoinRoomSuccess(waitUserMap(roomId)._1,waitUserMap(roomId)._2,roomId,getRoomActor(ctx,roomId))
                  waitUserMap.remove(roomId)
                }
              }

            }else{   //房间只有一个人
              roomInfoMap.put(roomId,(playerId,""))
              userActor ! UserActor.JoinRoomWait(roomId,getRoomActor(ctx,roomId))
              waitUserMap.put(roomId,(playerId,playerName,userActor))
            }
            Behaviors.same

          case t:UserLeftRoom =>
            roomInfoMap.remove(t.roomId)
            Behavior.same
          case ChildDead(roomId, childRef) =>
            log.info(s"Child${childRef.path}----$roomId is dead")
            roomInfoMap.remove(roomId)
            ctx.unwatch(childRef)
            Behaviors.same
        }
    }

  private def getRoomActor(ctx: ActorContext[Command], roomId: Long): ActorRef[RoomActor.Command] = {
    val childName = s"RoomActor-$roomId"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(RoomActor.create(roomId), childName)
      ctx.watchWith(actor, ChildDead(roomId, actor))
      actor
    }.upcast[RoomActor.Command]
  }
}
