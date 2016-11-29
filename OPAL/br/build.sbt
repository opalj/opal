name := "Bytecode Representation"

// We don't want the build to be aborted by inter-project links that are reported by
// scaladoc as errors if we publish the projects; hence, we do not use the
// standard compiler settings!
scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Representation")
scalacOptions in (Compile, console) := Seq("-deprecation")

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
