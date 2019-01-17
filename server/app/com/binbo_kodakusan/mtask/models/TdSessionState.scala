package com.binbo_kodakusan.mtask.models

case class TdSessionState(token: String, refreshToken: String,
                          expiresIn: Int, atTokenTook: Long)
