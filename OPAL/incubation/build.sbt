name := "Incubation"

// CODE IN THIS DIRECTORY IS NEVER EVEN CONSIDERED TO BE ALPHA QUALITY
version := "0.0.0-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature")

scalacOptions in (Compile, doc) ++= Opts.doc.title("Incubation") 