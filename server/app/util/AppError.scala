package util

import play.api.libs.json.JsValue

sealed trait AppError

object AppError {
  case class NoError() extends AppError
  case class Error(message: String) extends AppError
  case class Exception(exception: Throwable) extends AppError
  case class Json(json: JsValue) extends AppError
  case class TokenExpired(json: JsValue) extends AppError
}
