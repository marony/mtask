package com.binbo_kodakusan.mtask

import com.google.inject.Singleton
import javax.inject.Inject
import play.api.inject.{ApplicationLifecycle, Binding}
import play.api.{Configuration, Environment, Logger}

import scala.concurrent.Future

// application.conf
// play.modules.enabled += "com.binbo_kodakusan.mtask.MyModule"
class MyModule extends play.api.inject.Module {
  def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Logger.info("***** MyModule.bindings *****")
    Seq(
      bind[GlobalComponent].toSelf.eagerly
    )
  }
}

@Singleton
class GlobalComponent @Inject()
  (lifecycle: ApplicationLifecycle, environment: Environment) {

  // onStart
  val mode = environment.mode.toString
  Logger.info("START: ***** " + mode + " *****")

  lifecycle.addStopHook { () =>
    // onStop
    Logger.info("END: ***** " + mode + " *****")

    Future.successful(())
  }
}
