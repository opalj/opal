javaOptions in Global ++= Seq(
	"-Dorg.opalj.threads.CPUBoundTasks=8", 
	"-Dorg.opalj.threads.IOBoundTasks=16" 
)

//scalacOptions in ThisBuild += "-Xdisable-assertions"
