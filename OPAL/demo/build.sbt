name := "Demo"

version := "ALWAYS-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Demos") 

// We want to use different VM settings for OPAL
fork in run := true

javaOptions in run := Seq("-Xmx3G", "-Xms1024m", "-XX:+AggressiveOpts", "-Xnoclassgc", "-XX:InlineSmallCode=1500", "-XX:MaxInlineSize=52")