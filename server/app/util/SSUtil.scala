package util

import play.api.mvc.Session

// SessionUtil
object SSUtil {
  /**
    * セッションに値を追加
    *
    * ("state" -> "finish"), ("type" -> "1"), …
    *
    * @param session
    * @param ss
    * @return
    */
  def add(session: Session, ss: (String, String)*): Session = {
    var r = session
    ss.foreach { s =>
      r += s
    }
    r
  }

  /**
    * セッションから値を削除
    *
    * "state", "type", …
    *
    * @param session
    * @param ss
    * @return
    */
  def remove(session: Session, ss: String*): Session = {
    var r = session
    ss.foreach { s =>
      r -= s
    }
    r
  }
}
