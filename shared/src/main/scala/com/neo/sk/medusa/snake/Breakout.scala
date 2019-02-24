package com.neo.sk.medusa.snake

object Breakout {

  sealed trait SpotFB

//  case class BrickDetails(x:Int) extends  SpotFB
  case class PaddleDetails(id: Long, name: String, direction: Int, px: Int) extends  SpotFB
  case class BallDetails(x: Double, y: Double, sx: Double, sy: Double) extends  SpotFB

  case class GridDataSyncFB(
                           brickDetails: List[Int],
                           paddleDetails: PaddleDetails,
                           ballDetails: BallDetails
                           )
  case class PointFB(x: Int, y: Int) {
    def +(other: Point) = Point(x + other.x, y + other.y)

    def -(other: Point) = Point(x - other.x, y - other.y)

    def *(n: Int) = Point(x * n, y * n)

    def %(other: Point) = Point(x % other.x, y % other.y)
  }


  object BoundaryFB{
    val w = 120
    val h = 60
  }


}
