package com.neo.sk.breakout.http


import akka.actor.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout
import com.neo.sk.medusa.http.ResourceService

import scala.concurrent.ExecutionContextExecutor

trait HttpService extends
  ResourceService with
  LinkService{

  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler


  val snakeRoute = {
    (path("playGame") & get) {
      getFromResource("html/BreakOut.html")
    }
  }
  val adminRoute = {
    (path("admin") & get){
      getFromResource("html/BreakoutAdmin.html")
    }
  }


  val routes =
    pathPrefix("medusa") {
      snakeRoute~linkRoute~resourceRoutes~adminRoute
    }





}
