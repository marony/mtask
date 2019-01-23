package com.binbo_kodakusan.mtask.util

import cats.data._
import cats.implicits._
import play.api.mvc.Result

import scala.concurrent.{ExecutionContext, Future}

object EitherTUtil {
  /**
    * EitherT[Future, AppError, Result]がLeftの場合とFutureが例外の場合のエラー処理を記載する
    *
    * @param et
    * @param f1
    * @param f2
    * @param ex
    * @return
    */
  def eitherT2Error(et: EitherT[Future, AppError, Result],
                    leftF: AppError => Result, exceptionF: Throwable => Result)
                   (implicit ex: ExecutionContext): Future[Result] = {
    et.value.map {
      // 正常
      case Right(r: Result) => r
      // ロジックでのエラー
      case Left(e: AppError) => leftF(e)
    }.recover {
      // 例外
      case ex: Throwable => exceptionF(ex)
    }
  }
}
