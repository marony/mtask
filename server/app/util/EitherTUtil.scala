package util

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
                    f1: AppError => Result, f2: Throwable => Result)
                   (implicit ex: ExecutionContext): Future[Result] = {
    et.value.map {
      case Right(v) => v
      case Left(v) => f1(v)
    }.recover {
      case ex => f2(ex)
    }
  }
}
