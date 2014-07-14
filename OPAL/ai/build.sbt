name := "Abstract Interpretation Framework"

version := "0.0.1-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Abstract Interpretation Framework")

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.1"

////////////////////// "run" Configuration

fork in run := true

javaOptions in run := Seq("-Xmx3G", "-Xms1024m", "-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC", "-XX:+AggressiveOpts", "-Xnoclassgc")


////////////////////// (Unit) Tests

parallelExecution in Test := true

fork in Test := false


////////////////////// Integration Tests

parallelExecution in IntegrationTest := false

logBuffered in IntegrationTest := false

//javaOptions in IntegrationTest := Seq("-Xmx3G", "-ea", "-Xrs", "-esa", "-Xshare:off", "-XstartOnFirstThread", "-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC", "-XX:+AggressiveOpts")

//outputStrategy in IntegrationTest := Some(StdoutOutput)

//connectInput in IntegrationTest := false

//fork in IntegrationTest := true
