name := "OPAL-Developer Tools"

version := "0.8.0-SNAPSHOT"

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Developer Tools") 

scalacOptions in (Compile, console) := Seq()

fork in run := true