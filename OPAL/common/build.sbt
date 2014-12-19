name := "Common"

version := "1.0.0"

//scalacOptions in Compile += "-Xdisable-assertions"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL-Common") 

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.1"
