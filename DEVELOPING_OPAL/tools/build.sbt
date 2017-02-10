name := "OPAL-Developer Tools"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Developer Tools")
scalacOptions in (Compile, console) := Seq("-deprecation")

libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.102-R11"
libraryDependencies += "org.controlsfx" % "controlsfx" % "8.40.12"
libraryDependencies += "es.nitaur.markdown" % "txtmark" % "0.16"

fork := true

