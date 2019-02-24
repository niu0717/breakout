package com.neo.sk.breakout.Dao

import slick.ast.ColumnOption.AutoInc

import scala.concurrent.Future

/**
  * Created by dry on 2018/12/4.
  **/

case class UserInfo( id: Int, name: String ,password: String, disabled:Int)

trait UserInfoTable {
  import com.neo.sk.utils.DBUtil.driver.api._

  class UserInfoTable(tag: Tag) extends Table[UserInfo](tag, "TEST") {
    val id = column[Int]("ID",AutoInc,O.PrimaryKey)
    val name = column[String]("NAME")
    val password = column[String]("PASSWORD")
    val disabled = column[Int]("DISABLED")

    def * = (id, name, password, disabled) <> (UserInfo.tupled, UserInfo.unapply)
  }

  protected val UserInfoTableQuery = TableQuery[UserInfoTable]

}

object UserInfoRepo extends UserInfoTable {

  import com.neo.sk.utils.DBUtil.db
  import com.neo.sk.utils.DBUtil.driver.api._
//
//  def create(): Future[Unit] = {
//    db.run(eventDistributeTableQuery.schema.create)
//  }

//
//
//  def getAllEventInfo: Future[List[EventDistribute]] = {
//    db.run (eventDistributeTableQuery.to[List].result)
//  }
//
  def insertUserInfo(userInfo: UserInfo): Future[Int] = {
    db.run(UserInfoTableQuery.insertOrUpdate(userInfo))
  }

  def checkUserExist(name:String)={
    db.run(UserInfoTableQuery.filter(u => u.name === name).result.headOption)
  }

  def banUser(name: String)={
    db.run(UserInfoTableQuery.filter(u => u.name === name).map( m=>(m.disabled)).update(1))
  }
  def liftUser(name:String)={
    db.run(UserInfoTableQuery.filter(u => u.name === name).map( m=>(m.disabled)).update(0))
  }


}
