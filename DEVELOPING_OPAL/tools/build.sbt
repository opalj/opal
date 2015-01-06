name := "OPAL-Developer Tools"

version := "0.8.0-SNAPSHOT"

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Developer Tools") 

// We want to use different VM settings for OPAL
fork in run := true

javaOptions in run := 
	Seq("-Xmx3G",
		"-Xms1024m",
		"-XX:NewRatio=1",
		"-XX:SurvivorRatio=8",
		"-XX:+UseParallelGC",
		"-XX:+AggressiveOpts",
		"-Xnoclassgc",
		"-Dorg.opalj.threads.CPUBoundTasks=8",
		"-Dorg.opalj.threads.IOBoundTasks=48")