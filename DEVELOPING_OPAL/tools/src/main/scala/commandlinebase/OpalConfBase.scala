package org.opalj.support.info

import org.rogach.scallop.{ScallopConf, ScallopOption, ValueConverter}

class OpalConfBase(arguments: Seq[String]) extends ScallopConf(arguments){
    version("test 1.2.3 (c) 2012 Mr Placeholder")
    banner("""Usage: test [OPTION]... [tree|palm] [OPTION]... [tree-name]
             |test is an awesome program, which does something funny
             |Options:
             |""".stripMargin)
    footer("\nFor all other tricks, consult the documentation!")

    // Purity
    def getClassPath(): ScallopOption[String] = {
        createPlainScallopOption("classPath", "JAR file/Folder containing class files OR -JDK")
    }
    
    def getProjectDir(): ScallopOption[String] = {
        createPlainScallopOption("projectDir", "directory with project class files relative to cp")
    }

    def getLibClassPath(): ScallopOption[String] = {
        createPlainScallopOption("libClassPath", "directory with library class files relative to cp")
    }

    def getAnalysis(): ScallopOption[String] = {
        createChoiceScallopOption("analysis", Seq("L0", "L1", "L2"), Some("L2"), "Default: L2, the most precise analysis configuration")
    }

    def getFieldAssignability(): ScallopOption[String] = {
        createChoiceScallopOption("fieldAssignability", Seq("none", "L0", "L1", "L2"), null, "Default: Depends on analysis level")
    }

    def getEscape(): ScallopOption[String] = {
        createChoiceScallopOption("escape", Seq("none", "L0", "L1"), Some("L1"), "Default: L1, the most precise configuration")
    }

    def getDomain(): ScallopOption[String] = {
        createPlainScallopOption("domain", "class name of the abstract interpretation domain")
    }

    def getRater(): ScallopOption[String] = {
        createPlainScallopOption("rater", "class name of the rater for domain-specific actions")
    }

    def getCallGraph(): ScallopOption[String] = {
        createChoiceScallopOption("callGraph", Seq("CHA", "RTA", "PointsTo"), Some("RTA"), "Default: RTA")
    }

    def getEager(): ScallopOption[Boolean] = {
        createPlainScallopOption("eager", "supporting analyses are executed eagerly", Some(false))
    }

    def getNoJDK(): ScallopOption[Boolean] = {
        createPlainScallopOption("noJDK", "do not analyze any JDK methods", Some(false))
    }

    def getIndividual(): ScallopOption[Boolean] = {
        createPlainScallopOption("individual", "reports the purity result for each method", Some(false))
    }

    def getCloseWorld(): ScallopOption[Boolean] = {
        createPlainScallopOption("closedWorld", "uses closed world assumption, i.e. no class can be extended", Some(false))
    }

    def getLibrary(): ScallopOption[Boolean] = {
        createPlainScallopOption("library", "assumes that the target is a library", Some(false))
    }

    def getDebug(): ScallopOption[Boolean] = {
        createPlainScallopOption("debug", "enable debug output from PropertyStore", Some(false))
    }

    def getMulti(): ScallopOption[Boolean] = {
        createPlainScallopOption("multi", "analyzes multiple projects in the subdirectories of -cp", Some(false))
    }

    def getEval(): ScallopOption[String] = {
        createPlainScallopOption("eval", "path to evaluation directory")
    }

    def getPackages(): ScallopOption[String] = {
        createPlainScallopOption("packages", "colon separated list of packages, e.g. java/util:javax")
    }

    def getThreadNum(): ScallopOption[Int] = {
        createPlainScallopOption("threadnum", "number of threads to be used (0 for the sequential implementation)")
    }

    def getAnalysisName(): ScallopOption[String] = {
        createPlainScallopOption("analysisName", "analysisName which defines the analysis within the results file")
    }

    def getSchedulingStrategy(): ScallopOption[String] = {
        createPlainScallopOption("schedulingStrategy", "schedulingStrategy which defines the analysis within the results file")
    }


    // AnalysisApplication
    def getRenderConfig(): ScallopOption[Boolean] = {
        createPlainScallopOption("renderConfig", "prints the configuration", Some(false))
    }

    def getProjectConfig(): ScallopOption[String] = {
        createPlainScallopOption("projectConfig", "project type specific configuration options")
    }

    def getCompletelyLoadLibraries: ScallopOption[Boolean] = {
        createPlainScallopOption("completelyLoadLibraries", "the bodies of library methods are loaded", Some(false))
    }

    // CallGraph
    def getAnalysisAlgorithm(): ScallopOption[String] = {
        val algorithms = Seq("CHA", "RTA", "MTA", "FTA", "CTA", "XTA", "TypeBasedPointsTo", "PointsTo", "1-0-CFA", "1-1-CFA")
        createChoiceScallopOption("analysisAlgorithm", algorithms, null, "")
    }



    def getCallGraph(choices: Seq[String], defaultValue: Some[String], description: String): ScallopOption[String] = {
        choice(
            choices = choices,
            default = defaultValue,
            descr = description,
            noshort = true
        )
    }

    // Private methods
    private def createPlainScallopOption[T](name: String, description: String, default: Option[T] = None)(implicit converter: ValueConverter[T]): ScallopOption[T] = {
        opt[T](name = name, descr = description, default = default, noshort = true)
    }

    private def createChoiceScallopOption(name: String, choices: Seq[String], defaultValue: Some[String], description: String): ScallopOption[String] = {
        if(defaultValue == null) {
            choice(
                name = name,
                choices = choices,
                descr = description,
                noshort = true
            )
        } else {
            choice(
                name = name,
                choices = choices,
                default = defaultValue,
                descr = description,
                noshort = true
            )
        }
    }

    verify()
}

object Test {
    def main(args: Array[String]): Unit = {
        val confBase = new OpalConfBase(args.toIndexedSeq)
        confBase.printHelp()
    }
}