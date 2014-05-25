name := "Abstract Interpretation Framework"

version := "0.0.1-SNAPSHOT"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Abstract Interpretation Framework") 

libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % "1.0.1"

// We want to use different VM settings for OPAL
fork in run := true

javaOptions in run := Seq("-Xmx3G", "-Xms1014m", "-XX:CompileThreshold=3", "-XX:+AggressiveOpts", "-XX:+UseTLAB", "-XX:InlineSmallCode=2000", "-XX:MaxInlineSize=64")