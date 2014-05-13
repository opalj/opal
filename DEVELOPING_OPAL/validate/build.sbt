name := "Validates OPAL"

version := "0.8.0-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("VALIDATES OPAL") 

