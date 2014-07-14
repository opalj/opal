name := "Demo"

version := "ALWAYS-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Demos")

// We want to use different VM settings for OPAL
fork in run := true

//javaOptions in run := Seq("-Xmx3G", "-Xms1024m", "-XX:+AggressiveOpts", "-Xnoclassgc", "-XX:InlineSmallCode=1500", "-XX:MaxInlineSize=52")
//19.9secs... javaOptions in run := Seq("-Xmx8G", "-Xms1014m", "-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC", "-XX:+AggressiveOpts", "-Xnoclassgc", "-XX:InlineSmallCode=2048", "-XX:MaxInlineSize=64")
javaOptions in run := Seq("-Xmx3G", "-Xms1024m", "-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC", "-XX:+AggressiveOpts", "-Xnoclassgc")
