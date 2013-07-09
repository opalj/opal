import sbt._
import Keys._

object BatBuild extends Build {
	
	lazy val bat = Project(id="BAT", base=file(".")) aggregate(util, core, ext_ai, ext_dependencies, demo)

	lazy val util = Project(id="Core-Util", base=file("util")) 
	
	lazy val core = Project(id="Core", base=file("core")) dependsOn(util)

	lazy val ext_ai = Project(id="Ext-AbstractInterpretation", base=file("ext/ai")) dependsOn(core)
	
	lazy val ext_dependencies = Project(id="Ext-Dependencies", base=file("ext/dependencies")) dependsOn(core)			   

	lazy val demo = Project(id="Demo", base=file("demo")) dependsOn(core, ext_ai, ext_dependencies)
}

