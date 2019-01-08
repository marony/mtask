import sbt._

object ScalacOptions {
  lazy val scalacOptions = Seq(
    "-encoding", "UTF-8",
    "-language:_",

    "-deprecation", // @deprecatedなAPIが使われている箇所を警告します
    "-feature", // language feature の import が必要な箇所を警告します
    "-unchecked", // Enable additional warnings where generated code depends on     assumptions.
    "-Xlint", // scalac -Xlint:help
    "-Ywarn-dead-code", // Warn when dead code is identified.
    "-Ywarn-numeric-widen", // Warn when numerics are widened.
    "-Ywarn-unused", // Warn when local and private vals, vars, defs, and types are     are unused.
    "-Ywarn-unused-import", // Warn when imports are unused.
    "-Ywarn-value-discard", // Warn when non-Unit expression results are unused.
    //      "-Xfatal-warnings",
    "-Yno-adapted-args",
    "-Xfuture",
    "-Ypartial-unification"
  )
}
