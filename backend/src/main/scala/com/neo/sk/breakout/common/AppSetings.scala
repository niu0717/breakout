package com.neo.sk.breakout.common

import java.util.concurrent.TimeUnit

import com.neo.sk.utils.SessionSupport.SessionConfig
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

object AppSettings {
  private implicit class RichConfig(config: Config) {
    val noneValue = "none"

    def getOptionalString(path: String): Option[String] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getString(path))

    def getOptionalLong(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getLong(path))

    def getOptionalDurationSeconds(path: String): Option[Long] =
      if (config.getAnyRef(path) == noneValue) None
      else Some(config.getDuration(path, TimeUnit.SECONDS))
  }

  val log = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.parseResources("product.conf").withFallback(ConfigFactory.load())

  val appConfig = config.getConfig("app")

  val projectVersion = appConfig.getString("projectVersion")

  val httpInterface = appConfig.getString("http.interface")
  val httpPort = appConfig.getInt("http.port")

  val boundW = appConfig.getInt("bounds.w")
  val boundH = appConfig.getInt("bounds.h")

  val frameRate = appConfig.getInt("sync.frameRate")
  val syncDelay = appConfig.getInt("sync.delay")

  val appId = appConfig.getString("gameInfo.AppId")
  val secureKey = appConfig.getString("gameInfo.SecureKey")
  val gsKey = appConfig.getString("gameInfo.gsKey")
  val gameId = appConfig.getLong("gameInfo.gameId")
  val recordPath = appConfig.getString("record.recordPath")
  val isRecord = appConfig.getBoolean("record.isRecord")
  val isAuth = appConfig.getBoolean("isAuth")
  val esheepProtocol = appConfig.getString("esheepServer.protocol")
  val esheepHost = appConfig.getString("esheepServer.host")

  val autoBotConfig = appConfig.getConfig("autoBotSetting")
  val isAutoBotEnable = autoBotConfig.getBoolean("isAutoBotEnable")
  val autoBotNumber = autoBotConfig.getInt("autoBotNumber")
//  val botNameList = autoBotConfig.getStringList("botNameList").asScala
//  require(botNameList.lengthCompare(autoBotNumber) >= 0)

  val slickConfig = config.getConfig("slick.db")
  val slickUrl = slickConfig.getString("url")
  val slickUser = slickConfig.getString("user")
  val slickPassword = slickConfig.getString("password")
  val slickMaximumPoolSize = slickConfig.getInt("maximumPoolSize")
  val slickConnectTimeout = slickConfig.getInt("connectTimeout")
  val slickIdleTimeout = slickConfig.getInt("idleTimeout")
  val slickMaxLifetime = slickConfig.getInt("maxLifetime")

  val sessionConfig = {
    val sConf = config.getConfig("session")
    SessionConfig(
      cookieName = sConf.getString("cookie.name"),
      serverSecret = sConf.getString("serverSecret"),
      domain = sConf.getOptionalString("cookie.domain"),
      path = sConf.getOptionalString("cookie.path"),
      secure = sConf.getBoolean("cookie.secure"),
      httpOnly = sConf.getBoolean("cookie.httpOnly"),
      maxAge = sConf.getOptionalDurationSeconds("cookie.maxAge"),
      sessionEncryptData = sConf.getBoolean("encryptData")
    )
  }
}
