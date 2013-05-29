import sbt._
import Keys._

object BatBuild extends Build {
	
lazy val bat = Project(id="BAT",base=file(".")) aggregate(core,ext_ai,ext_dependencies,demo)
	
    lazy val core = Project(id = "BAT-Core",
                            base = file("core"))

   lazy val ext_ai = Project(id="BAT-Extension-AbstractInterpretation",base=file("ext/ai"))dependsOn(core)
   lazy val ext_dependencies = 
   Project(id="BAT-Extension-Dependencies",base=file("ext/dependencies")) dependsOn(core)			   

    lazy val demo = Project(id = "BAT-Demo",
                           base = file("demo")) dependsOn(core,ext_ai,ext_dependencies)
}

