package com.binbo_kodakusan.mtask.util

import play.api.Logger
import play.api.libs.ws.ahc.AhcCurlRequestLogger
import play.api.libs.ws.{BodyWritable, WSClient, WSRequest, WSResponse}
import play.mvc.Http.Response

import scala.concurrent.{ExecutionContext, Future}

object WSUtil {
  /**
    * Play WSオブジェクトを作成
    *
    * @param url
    * @param ws
    * @return
    */
  def url(url: String)(implicit ws: WSClient): WSRequest = {
    ws.url(url)
      .withRequestFilter(AhcCurlRequestLogger())
  }

  /**
    * Play WSのgetを呼ぶ
    * @param wsreq
    * @param f
    * @param ec
    * @return
    */
  def get(wsreq: WSRequest)
         (f: WSResponse => Response)
         (implicit ec: ExecutionContext)
  : Future[Response] = {

    ???

    wsreq.get.map { response =>
      Logger.info(response.body)
      f(response)
    }
  }

  /**
    * Play WSのpostを呼ぶ
    *
    * @param wsreq
    * @param body
    * @param f
    * @param ec
    * @tparam T
    * @return
    */
  def post[T: BodyWritable](wsreq: WSRequest, body: T,
                            f: WSResponse => Response)
                           (implicit ec: ExecutionContext)
    : Future[Response] = {

    ???

    wsreq.post(body).map { response: WSResponse =>
      Logger.info(response.body)
      f(response)
    }
  }
}
