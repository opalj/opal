name := "OPAL-Developer Tools"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Developer Tools") 
scalacOptions in (Compile, console) := Seq("-deprecation")

libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.102-R11" withJavadoc()
libraryDependencies += "org.controlsfx" % "controlsfx" % "8.40.12" withJavadoc()
libraryDependencies += "es.nitaur.markdown" % "txtmark" % "0.16" withJavadoc()
libraryDependencies += "org.choco-solver" % "choco-solver" % "4.0.2" withJavadoc()
libraryDependencies += "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv" % "2.8.6" withJavadoc()

fork in run := true