package util

import play.api.Logger

object LogUtil {
  private val CrLf = System.getProperty("line.separator")

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
