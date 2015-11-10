name := "Fixpoint Computations Framework Analyses"

version := "0.0.1-SNAPSHOT"

// We don't want the build to be aborted by inter-project links that are reported by
// scaladoc as errors if we publish the projects; hence, we do not use the 
// standard compiler settings!
scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Fixpoint Computations Framework Analyses") ++ Seq("-groups", "-implicits")

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.4"

libraryDependencies += "net.ceedubs" %% "ficus" % "1.1.2"

////////////////////// "run" Configuration

fork in run := true

