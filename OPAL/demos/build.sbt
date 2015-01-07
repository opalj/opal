name := "Demos"

version := "ALWAYS-SNAPSHOT"

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Demos")

// We want to use a different VM 
fork in run := true
