package com.neo.sk.breakout

import akka.actor.{ActorSystem, Scheduler}
import akka.actor.typed.ActorRef
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.neo.sk.breakout.core.{RoomManager, UserManager}
import com.neo.sk.breakout.http.HttpService
import akka.actor.typed.scaladsl.adapter._

object Boot extends HttpService{
  import concurrent.duration._
  import com.neo.sk.medusa.common.AppSettings._


  override implicit val system: ActorSystem = ActorSystem("medusa", config)
  // the executor should not be the default dispatcher.
  override implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  override implicit val materializer: ActorMaterializer = ActorMaterializer()

  override implicit val scheduler: Scheduler = system.scheduler

  override implicit val timeout: Timeout = Timeout(20 seconds) // for actor asks

  val log: LoggingAdapter = Logging(system, getClass)
  //  val delayer = system.spawn(Delayer.start, "Delayer")
  val userManager: ActorRef[UserManager.Command] = system.spawn(UserManager.behaviors,"UserManager")
  val roomManager: ActorRef[RoomManager.Command] = system.spawn(RoomManager.behaviors,"RoomManager")
//  CountUtils.initCount()

  def main(args: Array[String]) {
    log.info("Starting.")
    Http().bindAndHandle(routes, httpInterface, httpPort)
    log.info(s"Listen to the $httpInterface:$httpPort")
    //    delayer ! Start
    log.info("Done.")
  }



}
