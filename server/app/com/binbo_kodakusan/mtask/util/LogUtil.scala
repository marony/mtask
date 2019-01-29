package com.binbo_kodakusan.mtask.util

import play.api.Logger

object LogUtil {
  private val CrLf = System.getProperty("line.separator")

  /**
    * 例外オブジェクトをいい感じにログ出力する
    *
    * @param ex
    * @param prefix
    */
  def errorEx(ex: Throwable, prefix: String = ""): Unit = {
    val str = new StringBuilder(if (prefix == "") "" else prefix + ": ")
    str ++= ex.toString
    str ++= CrLf
    for (s <- ex.getStackTrace) {
      str ++= s.toString
      str ++= CrLf
    }
    Logger.error(str.toString())
  }
}

/**
  * Logging("関数名", { 処理 })
  * で関数のログを出力する
  */
object Logging {
  def apply[R](name: String, f: => R): R = {
    try {
      Logger.info(s"START: $name")
      f
    }
    finally {
      Logger.info(s"EBD: $name")
    }
  }
}