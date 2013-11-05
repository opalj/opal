version := "0.0.1"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("The BATAI Framework") 
 
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2.3"