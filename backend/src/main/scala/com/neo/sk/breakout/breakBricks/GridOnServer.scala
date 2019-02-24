package com.neo.sk.breakout.breakBricks

import akka.actor.typed.ActorRef
import com.neo.sk.breakout.core.RoomActor
import com.neo.sk.medusa.snake.Breakout.{BallDetails, PaddleDetails, PointFB}
import com.neo.sk.medusa.snake.{Breakout, ProtocolFB}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

class GridOnServer( val boundary: PointFB, roomActor:ActorRef[RoomActor.Command]) {
  private[this] val log = LoggerFactory.getLogger(this.getClass)
  private[this] var waitingJoin = Map.empty[String, String]
  private [this] val brickList = ListBuffer.empty[Int]

  def addUser(id: String, name: String) = waitingJoin += (id -> name)

  def init(level: Int)={
    for(i <- 0 to 14){
      brickList.append(0)
    }
    val brickDetails:List[Int] = brickList.toList
    val paddleDetails:PaddleDetails = PaddleDetails(0,"",1,100)
    val ballDetails:BallDetails = BallDetails(120,120,(1+0.4*level),(1.5-0.4*level))
    val uid = 1L
    ProtocolFB.GridDataMessage(
      uid,
      Breakout.GridDataSyncFB(brickDetails, paddleDetails,ballDetails)
    )
  }

  def updateBricks(bricks:List[Int])={

    for( i <- 0 until brickList.length){
      brickList.update(i,bricks.apply(i))
    }
  }
  def updateBall()={

  }
  def updatePaddle()={

  }

//  def getSyncData={
//    var brickDetails:List[Int] = brickList.toList
//    var paddleDetails:PaddleDetails = PaddleDetails()
//    var ballDetails:BallDetails = BallDetails()
//    val uid = 1L
//    ProtocolFB.GridDataMessage(
//      uid,
//      Breakout.GridDataSyncFB(brickDetails, paddleDetails,ballDetails)
//    )
//  }



}
