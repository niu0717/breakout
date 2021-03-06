package com.neo.sk.medusa.http

import akka.actor.{ActorRef, ActorSystem, Scheduler}
import akka.http.scaladsl.server.Directives._
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{Materializer, OverflowStrategy}
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor

/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:27 PM
  */
trait HttpService extends
  ResourceService
  {


  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler



  val snakeRoute = {
    (path("playGame") & get) {
      getFromResource("html/BreakOut.html")
    } ~ (path("watchGame") & get){
      getFromResource("html/BreakOut.html")
    } ~ (path("watchRecord") & get){
      getFromResource("html/BreakOut.html")
    }
  }


  val routes =
    pathPrefix("medusa") {
       snakeRoute ~ resourceRoutes
    }




}
