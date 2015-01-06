name := "Common"

version := "0.8.0-SNAPSHOT"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL-Common") 

//scalacOptions in Compile += "-Xdisable-assertions"

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.1"
