name := "Incubation"

// INCUBATION CODE IS NEVER EVEN CONSIDERED TO BE ALPHA QUALITY
version := "ALWAYS-SNAPSHOT"

scalacOptions in (Compile, doc) := Opts.doc.title("Incubation") 

scalacOptions in (Compile, console) := Seq()

libraryDependencies += "net.ceedubs" %% "ficus" % "1.1.2"

// We want to use a different VM 
fork in run := true