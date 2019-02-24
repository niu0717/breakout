package com.neo.sk.breakout.core


import com.neo.sk.breakout.Dao.{UserInfo, UserInfoRepo}

import scala.util.{Failure, Success}

object test {
  def main(args: Array[String]): Unit = {

    import com.neo.sk.breakout.Boot.executor

    UserInfoRepo.insertUserInfo(UserInfo(-1,"12312","12312",0))
    // .map { u =>
//      if(u.isDefined)println(12)
//      else println(21)
//    }
  }
}
