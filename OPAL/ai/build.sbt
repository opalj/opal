name := "Abstract Interpretation Framework"

version := "0.0.1-SNAPSHOT"

// We don't want the build to be aborted by inter-project links that are reported by
// scaladoc as errors if we publish the projects; hence, we do not use the 
// standard compiler settings!
scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Abstract Interpretation Framework")

//scalacOptions in Compile ++= Seq("-Xdisable-assertions", "-Yinline", "-Yconst-opt", "-Ydead-code")

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.1"

////////////////////// "run" Configuration

fork in run := true

javaOptions in run := Seq(
	"-Xmx2G", "-Xms1024m", "-Xnoclassgc",
	"-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC", "-XX:+AggressiveOpts", 
	"-Dorg.opalj.threads.CPUBoundTasks=8")



////////////////////// (Unit) Tests

parallelExecution in Test := true

fork in Test := false


////////////////////// Integration Tests

logBuffered in IntegrationTest := false

