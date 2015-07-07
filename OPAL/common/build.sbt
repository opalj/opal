name := "Common"

version := "0.8.1-SNAPSHOT"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL-Common") 

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.4"

libraryDependencies += "net.ceedubs" %% "ficus" % "1.1.2"

//scalacOptions in Compile += "-Xdisable-assertions"