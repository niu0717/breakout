package com.neo.sk.medusa.snake.scalajs

import com.neo.sk.medusa.snake.ProtocolFB.{LoginRspFB, UserInfoFB, UserInfoRspFB}
import com.neo.sk.medusa.snake.scalajs.common.Routes
import com.neo.sk.medusa.snake.scalajs.utils.{Http, JsFunc}
import io.circe.syntax._
import io.circe.generic.auto._
import io.circe.parser._
import sun.security.util.Password

object ConnectBackend {
  import scala.concurrent.ExecutionContext.Implicits.global

  def registerInfoF(name:String,password:String)={
    val bodyStr = UserInfoFB(name,password).asJson.noSpaces
//    println(bodyStr)

    Http.postJsonAndParse[UserInfoRspFB](Routes.UserRoutes.registerUrl,bodyStr).map{
      case Right(rsp) =>
        if(rsp.errorCode==0){
          JsFunc.alert("注册成功")
        }else{
          JsFunc.alert(rsp.msg)
        }
      case Left(e) =>{
        JsFunc.alert("未知错误: "+e)
      }
    }
  }

  def login(name:String,password:String)={
    val bodyStr = UserInfoFB(name,password).asJson.noSpaces
    Http.postJsonAndParse[LoginRspFB](Routes.UserRoutes.loginUrl,bodyStr).map{
      case Right(rsp) =>
        if(rsp.errorCode==1){
          1
        }else{
          0
        }
      case Left(e) =>{
        0
      }
    }
  }


}
