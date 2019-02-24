package com.neo.sk.breakout.http

import akka.actor.{ActorSystem, Scheduler}
import akka.stream.Materializer
import org.slf4j.LoggerFactory
import com.neo.sk.utils.ServiceUtils
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.ws.Message
import akka.stream.scaladsl.Flow
import com.neo.sk.breakout.Boot.{executor, userManager}
import com.neo.sk.breakout.core.UserManager
import com.neo.sk.medusa.http.SessionBase
import com.neo.sk.medusa.snake.ProtocolFB.{LoginRspFB, UserInfoFB, UserInfoRspFB}
import akka.http.scaladsl.server.Directives._
import akka.util.Timeout
import com.neo.sk.breakout.Dao.{UserInfo, UserInfoRepo}

import scala.concurrent.Future
import scala.util.{Failure, Success}


trait LinkService extends ServiceUtils with SessionBase {
  import io.circe._
  import io.circe.generic.auto._

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler

  implicit val system: ActorSystem

  implicit val materializer: Materializer

  private[this] val log = LoggerFactory.getLogger("LinkServiceFB")

  private val playGameRoute = path("playGame") {
    parameter('playerId.as[String], 'playerName.as[String], 'roomId.as[Long]) {
      (playerId, playerName, roomId) =>
        val flowFuture: Future[Flow[Message, Message, Any]] =
          userManager ? (UserManager.GetWebSocketFlow(playerId, playerName, roomId, _))
        dealFutureResult(
          flowFuture.map(r => handleWebSocketMessages(r))
        )
    }
  }

  private val registerRoute = (path(pm="register") & post){
        entity(as[Either[Error,UserInfoFB]]){
          case Left(error) =>
            println("error: "+ error)
            complete()
          case Right(userInfo) =>
            println("register_userInfo: "+userInfo)
            dealFutureResult(
              UserInfoRepo.checkUserExist(userInfo.name).map { u =>
                if(u.isDefined){
                  complete(UserInfoRspFB(1,"用户已存在"))
                }else{
                  UserInfoRepo.insertUserInfo(UserInfo(-1,userInfo.name,userInfo.password,0))
                  complete(UserInfoRspFB(0, "注册成功"))
                }
              }
            )
        }
  }

  private val loginRoute = (path(pm="login") & post){
    entity(as[Either[Error,UserInfoFB]]){
      case Left(error) =>
        println("error: "+ error)
        complete()
      case Right(userInfo) =>
        println("login_userInfo: "+userInfo)
        dealFutureResult(
          UserInfoRepo.checkUserExist(userInfo.name).map { u =>
            if(u.isDefined && u.get.disabled==0){
              complete(LoginRspFB(1,"登陆成功"))
            }else{
              complete(LoginRspFB(0,"用户不存在"))
            }
          }
        )
    }
  }

  val linkRoute = (pathPrefix("link")) {
    playGameRoute~registerRoute~loginRoute
  }







}