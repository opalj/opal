name := "Abstract Interpretation Framework"

version := "0.0.1-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Abstract Interpretation Framework") 

