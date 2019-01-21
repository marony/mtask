package com.binbo_kodakusan.mtask.models

case class SessionState(token: String, refreshToken: String,
                        expiresIn: Int, atTokenTook: Long)
