import sbt._
import Keys._

object BatBuild extends Build {
	
	lazy val bat = Project(id="BAT", base=file(".")) aggregate(
		util, 
		core, 
		ext_dependencies, 
		ext_ai, 
		ext_tools,
		demo, 
		incubation) 

	lazy val util = Project(id="Core-Util", base=file("util")) 
	
	lazy val core = Project(id="Core", base=file("core")) dependsOn(util)

	lazy val ext_dependencies = Project(id="Ext-Dependencies", base=file("ext/dependencies")) dependsOn(core % "test->test;compile->compile")			   

	lazy val ext_ai = Project(id="Ext-AbstractInterpretation", base=file("ext/ai")) dependsOn(core % "test->test;compile->compile", ext_dependencies % "test->compile")

	/* Projects that facilitate the development of analyses. */

	lazy val ext_tools = Project(id="Ext-Tools", base=file("ext/tools")) dependsOn(core, ext_ai, ext_dependencies)
	
	lazy val demo = Project(id="Demo", base=file("demo")) dependsOn(core, ext_ai, ext_dependencies)
	
	lazy val incubation = Project(id="Incubation", base=file("incubation")) dependsOn(core, ext_ai, ext_dependencies)
}

