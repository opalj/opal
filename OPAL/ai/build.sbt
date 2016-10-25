name := "Abstract Interpretation Framework"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Abstract Interpretation Framework") ++ Seq("-groups", "-implicits")
scalacOptions in (Compile, console) := Seq("-deprecation")

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.5.9"

fork in run := true
