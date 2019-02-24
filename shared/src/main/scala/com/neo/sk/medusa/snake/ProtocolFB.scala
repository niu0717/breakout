package com.neo.sk.medusa.snake

import com.neo.sk.medusa.snake.Breakout.GridDataSyncFB

object ProtocolFB {

  sealed trait WsMsgSource

  case object CompleteMsgServer extends WsMsgSource
  case class FailMsgServer(ex: Throwable) extends WsMsgSource
  case object LagSet extends WsMsgSource

  sealed trait GameMessageFB extends WsMsgSource

  case class GridDataMessage(
                              uid: Long,
                              data: GridDataSyncFB
                            ) extends GameMessageFB

  case class NewBJoined(id: Long, name: String) extends GameMessageFB
  case class JoinGameSuccess(username: String, name: String,roomId:Long) extends GameMessageFB
  case class WaitOther(msg:String) extends GameMessageFB
  case class watchOther(msg:String) extends GameMessageFB
  case class PlayToWatch() extends  GameMessageFB
  case class PaddleAction(playerName:String,keyCode:Int,px:Int,KeyCount:Int) extends GameMessageFB
  case class SyncBallDetails(x: Double, y: Double, sx: Double, sy: Double) extends  GameMessageFB
  case class PlayerList(player1:String,player2:String) extends  GameMessageFB
  case class Score(player1:Int) extends  GameMessageFB
  case class SyncBarrage(msg:String) extends  GameMessageFB
  case class OneLeft() extends  GameMessageFB

  case class GridDataForWatch(
                             watcher:String,
                             watchInfo:PaddleAction
                             )extends  GameMessageFB

  case class PlayerLife(myself:Long,player1:Long, player1Life:Boolean, player2:Long, player2Life:Boolean) extends GameMessageFB


  sealed trait WsSendMsgFB
  case object WsSendComplete extends WsSendMsgFB
  case class WsSendFailed(ex: Throwable) extends WsSendMsgFB

  sealed trait UserAction extends WsSendMsgFB

  case class Key(playerName: String, keyCode: Int,px:Int,KeyCount:Int) extends UserAction
  case class TextInfo(id: String, info: String) extends UserAction
  case class NetTest(id: String, createTime: Long) extends UserAction
  case class JoinGame(id: String, name: String,roomId:String) extends UserAction
  case class ChangeModel(player1:String, player1Life:Boolean, player2:String, player2Life:Boolean, player1Score:Int) extends  UserAction
  case class GetBallDetails(x: Double, y: Double, sx: Double, sy: Double) extends  UserAction
  case class SendBarrage(msg:String) extends  UserAction
  case class BackInit() extends  UserAction


//  trait
  case class UserInfoFB(
                     name:String,
                     password:String
                     )
  case class UserInfoRspFB(
                        errorCode:Int,
                        msg:String
                        )
  case class LoginRspFB(
                         errorCode:Int,
                         msg:String
                       )

}
