package com.neo.sk.medusa.snake.scalajs

import com.neo.sk.medusa.snake.Breakout.{BallDetails, PointFB}
import com.neo.sk.medusa.snake.ProtocolFB.{ChangeModel, PlayerLife}
import sun.util.logging.PlatformLogger.Level

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class GridOnWeb {
//  val level = 1
  var actionMap = mutable.HashMap.empty[String,(Int,Int,Int)]  //用户Id--->移动方向，px，第n次操作
  var brick = Array.ofDim[Int](1,1)
  var nowModel = ChangeModel("0",false,"0",false,0)
  var ballInfo = BallDetails(300.0,200.0,3.4,2.7)
  var playerList =Array("","")
  var barrageInfo = mutable.HashMap.empty[String,(Double,Double,Int)]
//  var ballInfo = BallDetails(120.0,120.0,(1+0.4*level),(1.5-0.4*level))
  def addActionMap(playerName:String,direction:Int,px:Int,KeyCount:Int) = {

    actionMap += (playerName->(direction,px,KeyCount))

  }
  def updateActionMap(playerName:String,direction:Int,px:Int,KeyCount:Int)={
    actionMap.update(playerName,(direction,px,KeyCount))
  }

  def updateModel(player1:String,player2:String, playerLife:Boolean)={
    nowModel = ChangeModel(player1,true,player2,false,0)
  }

  def brickInit(level: Int)={
    brick = Array.ofDim[Int](level+2,5)
    ballInfo = BallDetails(300.0,200.0,(3.4+1.6*level),(3.0-0.4*level))
  }

  def updateBrickInfo(i:Int,j:Int)={
    brick(i)(j) = 1
  }

  def ballInit()={

  }



  def updateBallInfo(bx:Double,by:Double,bsx:Double,bsy:Double)={
    ballInfo = BallDetails(bx,by,bsx,bsy)
  }



}
