
scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("BAT-Core Utility Functionality") 

libraryDependencies += "org.scala-lang" % "scala-reflect" % "2.10.3" 
