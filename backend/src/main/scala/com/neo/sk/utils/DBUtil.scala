package com.neo.sk.utils

import com.zaxxer.hikari.HikariDataSource
import org.h2.jdbcx.JdbcDataSource
import org.slf4j.LoggerFactory
import slick.driver.PostgresDriver.api._
import slick.jdbc.H2Profile
//import slick.jdbc.PostgresProfile.api._
import com.neo.sk.medusa.common.AppSettings._

/**
 * User: Taoz
 * Date: 2/9/2015
 * Time: 4:33 PM
 */
object DBUtil {
  val log = LoggerFactory.getLogger(this.getClass)
  private val dataSource = createDataSource()


  private def createDataSource() = {

    val dataSource = new JdbcDataSource
    dataSource.setURL(slickUrl)
    dataSource.setUser(slickUser)
    dataSource.setPassword(slickPassword)
    val hikariDS = new HikariDataSource()
    hikariDS.setDataSource(dataSource)
    hikariDS.setMaximumPoolSize(slickMaximumPoolSize)
    hikariDS.setConnectionTimeout(slickConnectTimeout)
    hikariDS.setIdleTimeout(slickIdleTimeout)
    hikariDS.setMaxLifetime(slickMaxLifetime)
    hikariDS.setAutoCommit(true)
    hikariDS
  }

  val driver = H2Profile

  import driver.api.Database

  val db: Database = Database.forDataSource(dataSource, Some(slickMaximumPoolSize))




}