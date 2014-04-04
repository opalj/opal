
scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature")

scalacOptions in (Compile, doc) ++= Opts.doc.title("Incubation") 