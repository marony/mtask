import sbtcrossproject.{crossProject, CrossType}

val scalaV = "2.12.8"
val diodeV = "1.1.4"

val commonScalacOptions = ScalacOptions.scalacOptions

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

lazy val server = (project in file("server")).settings(commonSettings).settings(
  scalaJSProjects := Seq(client),
  pipelineStages in Assets := Seq(scalaJSPipeline),
  pipelineStages := Seq(digest, gzip),
  // triggers scalaJSPipeline when using compile or continuous compilation
  compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
  libraryDependencies ++= Seq(
    "org.postgresql" % "postgresql" % "42.2.5",
    "com.typesafe.play" %% "play-slick" % "3.0.3",
    "com.typesafe.play" %% "play-slick-evolutions" % "3.0.3",
    "com.typesafe.slick" %% "slick-codegen" % "3.2.1",
    "com.vmunier" %% "scalajs-scripts" % "1.1.2",
    "io.suzaku" %%% "diode" % diodeV,

    "org.typelevel" %% "cats-core" % "1.5.0",

    "org.webjars" %% "webjars-play" % "2.6.3",
    "org.webjars" % "bootstrap" % "4.2.1",

    ws,
    guice,
    specs2 % Test
  ),

  // Slick CodeGen
  slickCodeGen := { slickCodeGenTask.value }, // register manual sbt command
//  sourceGenerators in Compile += { slickCodeGenTask.taskValue }, // register automatic code generation on every compile, remove for only manual use

  // Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
  EclipseKeys.preTasks := Seq(compile in Compile)
).enablePlugins(PlayScala, WebScalaJSBundlerPlugin).
  dependsOn(sharedJvm)

lazy val client = (project in file("client")).settings(commonSettings).settings(
  scalacOptions ++= commonScalacOptions ++
    (if (scalaJSVersion.startsWith("0.6."))
      Seq("-P:scalajs:sjsDefinedByDefault")
    else Nil),
  scalaJSUseMainModuleInitializer := true,
  libraryDependencies ++= Seq(
    "org.scala-js" %%% "scalajs-dom" % "0.9.5",
    "com.github.japgolly.scalajs-react" %%% "core" % "1.3.1",
    "com.github.japgolly.scalajs-react" %%% "extra" % "1.3.1",
    "io.suzaku" %%% "diode" % diodeV,
    "io.suzaku" %%% "diode-devtools" % diodeV,
    "io.suzaku" %%% "diode-react" % s"$diodeV.131",
    "io.suzaku" %%% "boopickle" % "1.3.0"
  ),
  npmDependencies in Compile ++= Seq(
      "react" -> "16.5.1",
      "react-dom" -> "16.5.1",
  )
).enablePlugins(ScalaJSPlugin, ScalaJSWeb, ScalaJSBundlerPlugin).
  dependsOn(sharedJs)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
  .settings(commonSettings)
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val commonSettings = Seq(
  scalacOptions ++= ScalacOptions.scalacOptions,
  scalaVersion := scalaV,
  organization := "com.binbo_kodakusan"
)

// code generation task
lazy val slickCodeGen = TaskKey[Seq[File]]("gen-tables")
lazy val slickCodeGenTask = Def task {
  val dir = sourceManaged.value
  val cp = (dependencyClasspath in Compile).value
  val r = (runner in Compile).value
  val s = streams.value

  val outputDir = (dir / "slick").getPath // place generated files in sbt's managed sources folder
  val url = "jdbc:postgresql://localhost/mtask" // connection info for a pre-populated throw-away, in-memory db for this demo, which is freshly initialized on every run
  val jdbcDriver = "org.postgresql.Driver"
  val slickDriver = "slick.jdbc.PostgresProfile"
  val pkg = ""
  val user = "mtask"
  val password = "ksatm"
  r.run("slick.codegen.SourceCodeGenerator", cp.files,
    Array(slickDriver, jdbcDriver, url, outputDir, pkg,
      user, password), s.log).failed foreach (sys error _.getMessage)
  val fname = outputDir + "/Tables.scala"
  Seq(file(fname))
}

// loads the server project at sbt startup
onLoad in Global := (onLoad in Global).value andThen {s: State => "project server" :: s}
