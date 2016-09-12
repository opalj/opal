name := "Abstract Interpretation Framework"

// We don't want the build to be aborted by inter-project links that are reported by
// scaladoc as errors if we publish the projects; hence, we do not use the 
// standard compiler settings!
scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Abstract Interpretation Framework") ++ Seq("-groups", "-implicits")
scalacOptions in (Compile, console) := Seq("-deprecation")

fork in run := true

