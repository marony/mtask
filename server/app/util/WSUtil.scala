package util

import play.api.libs.ws.ahc.AhcCurlRequestLogger
import play.api.libs.ws.{WSClient, WSRequest}

object WSUtil {
  def url(url: String)(implicit ws: WSClient): WSRequest = {
    ws.url(url)
      .withRequestFilter(AhcCurlRequestLogger())
  }
}
