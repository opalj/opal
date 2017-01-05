name := "Abstract Interpretation Framework"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Abstract Interpretation Framework") ++ Seq("-groups", "-implicits")
scalacOptions in (Compile, console) := Seq("-deprecation")

// TODO Move the Java annotations to test-fixtures!
unmanagedSourceDirectories in Test := (javaSource in Test).value :: (scalaSource in Test).value :: Nil

fork in run := true
