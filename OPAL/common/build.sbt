name := "Common"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL-Common") 
scalacOptions in (Compile, console) := Seq("-deprecation")

libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.5"
//[FICUS HAS MOVED TO COM.IHEART]libraryDependencies += "net.ceedubs" %% "ficus" % "1.1.2"
libraryDependencies += "com.iheart" %% "ficus" % "1.2.6"


