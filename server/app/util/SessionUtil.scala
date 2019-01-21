package util

import com.binbo_kodakusan.mtask.{Constants, models}
import com.binbo_kodakusan.mtask.models.{SessionState}
import play.api.mvc.Session

// SessionUtil
object SessionUtil {
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
    * @returnop9
    */
  def remove(session: Session, ss: String*): Session = {
    var r = session
    ss.foreach { s =>
      r -= s
    }
    r
  }

  /**
    * Toodledoの情報をセッションに保存
    *
    * @param session
    * @param token
    * @param refresh_token
    * @param expires_in
    * @param at_token_took
    * @tparam T
    * @return
    */
  def setTdSession(session: Session, state: SessionState): Session = {
    add(session,
      Constants.SessionName.TD_TOKEN -> state.token,
      Constants.SessionName.TD_REFRESH_TOKEN -> state.refreshToken,
      Constants.SessionName.TD_EXPIRES_IN -> state.expiresIn.toString,
      Constants.SessionName.TD_AT_TOKEN_TOOK -> state.atTokenTook.toString)
  }

  /**
    * セッションからToodledoの情報を取得
    *
    * @param session
    * @return
    */
  def getTdSessionState(session: Session): Option[SessionState] = {
    for {
      token <- session.get(Constants.SessionName.TD_TOKEN)
      refreshToken <- session.get(Constants.SessionName.TD_REFRESH_TOKEN)
      expiresIn <- session.get(Constants.SessionName.TD_EXPIRES_IN)
      atTokenTook <- session.get(Constants.SessionName.TD_AT_TOKEN_TOOK)
    } yield models.SessionState(token, refreshToken, expiresIn.toInt, atTokenTook.toLong)
  }
}
