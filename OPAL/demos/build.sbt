name := "Demos"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Demos")
scalacOptions in (Compile, console) := Seq("-deprecation")

unmanagedSourceDirectories in Compile :=  (javaSource in Compile).value :: (scalaSource in Compile).value :: Nil

fork in run := true
