name := "Abstract Interpretation Framework"

version := "0.0.2-SNAPSHOT"

// We don't want the build to be aborted by inter-project links that are reported by
// scaladoc as errors if we publish the projects; hence, we do not use the 
// standard compiler settings!
scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Abstract Interpretation Framework") ++ Seq("-groups", "-implicits")

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.4"

libraryDependencies += "net.ceedubs" %% "ficus" % "1.1.2"

////////////////////// "run" Configuration

fork in run := true

