name := "Abstract Interpretation Framework"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Abstract Interpretation Framework") ++ Seq("-groups", "-implicits")
scalacOptions in (Compile, console) := Seq("-deprecation")

fork in run := true

