package com.binbo_kodakusan.mtask.models.td

case class SessionState(token: String, refreshToken: String,
                        expiresIn: Int, atTokenTook: Long)
