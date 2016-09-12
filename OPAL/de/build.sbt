name := "Dependencies Extraction Library"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Dependencies Extraction Library")

scalacOptions in (Compile, console) := Seq("-deprecation") 