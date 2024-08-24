package org.opalj.support.info

import org.rogach.scallop.{ScallopConf, ScallopOption}

class OpalConfBase(arguments: Seq[String]) extends ScallopConf(arguments){
    version("test 1.2.3 (c) 2012 Mr Placeholder")
    banner("""Usage: test [OPTION]... [tree|palm] [OPTION]... [tree-name]
             |test is an awesome program, which does something funny
             |Options:
             |""".stripMargin)
    footer("\nFor all other tricks, consult the documentation!")

    // Purity
    def getClassPath(description: String): ScallopOption[String] = {
        getPlainScallopOption("classPath", description)
    }

    def getProjectDir(description: String): ScallopOption[String] = {
        getPlainScallopOption("projectDir", description)
    }

    def getLibClassPath(description: String): ScallopOption[String] = {
        getPlainScallopOption("libClassPath", description)
    }

    def getAnalysis(choices: Seq[String], defaultValue: Some[String], description: String): ScallopOption[String] = {
        getChoiceScallopOption("analysis", choices, defaultValue, description)
    }

    def getFieldAssignability(choices: Seq[String], defaultValue: Some[String], description: String): ScallopOption[String] = {
        getChoiceScallopOption("fieldAssignability", choices, defaultValue, description)
    }

    val escape: ScallopOption[String] = choice(
        choices = Seq("none", "L0", "L1"),
        default = Some("L1"),
        descr = "Default: L1, the most precise configuration",
        noshort = true
    )
    val domain: ScallopOption[String] = opt[String](descr = "class name of the abstract interpretation domain", noshort = true)
    val rater: ScallopOption[String] = opt[String](descr = "class name of the rater for domain-specific actions ", noshort = true)
    val callGraph: ScallopOption[String] = choice(
        choices = Seq("CHA", "RTA", "PointsTo"),
        default = Some("RTA"),
        descr = "Default: RTA",
        noshort = true
    )
    val eager: ScallopOption[Boolean] = opt[Boolean](descr = "supporting analyses are executed eagerly", default = Some(false), noshort = true)
    val noJDK:  ScallopOption[Boolean] = opt[Boolean](descr = "do not analyze any JDK methods", default = Some(false), noshort = true)
    val individual:  ScallopOption[Boolean] = opt[Boolean](descr = "reports the purity result for each method", default = Some(false), noshort = true)
    val closedWorld:  ScallopOption[Boolean] = opt[Boolean](descr = "uses closed world assumption, i.e. no class can be extended", default = Some(false), noshort = true)
    val library:  ScallopOption[Boolean] = opt[Boolean](descr = "assumes that the target is a library", default = Some(false), noshort = true)
    val debug:  ScallopOption[Boolean] = opt[Boolean](descr = "enable debug output from PropertyStore", default = Some(false), noshort = true)
    val eval: ScallopOption[String] = opt[String](descr = "path to evaluation directory", noshort = true)
    val packages: ScallopOption[String] = opt[String](descr = "colon separated list of packages, e.g. java/util:javax", noshort = true)
    val numThreads: ScallopOption[Int] = opt[Int](descr = "number of threads to be used, 0 for the sequential implementation", noshort = true)
    val analysisName: ScallopOption[String] = opt[String](descr = "analysisName which defines the analysis within the results file", noshort = true)
    val schedulingStrategy: ScallopOption[String] = opt[String](descr = "schedulingStrategy which defines the analysis within the results file", noshort = true)

    // AnalysisApplication
    val renderConfig:  ScallopOption[Boolean] = opt[Boolean](descr = "prints the configuration", default = Some(false), noshort = true)
    val projectConfig: ScallopOption[String] = opt[String](descr = "project type specific configuration options", noshort = true)
    val completelyLoadLibraries: ScallopOption[Boolean] = opt[Boolean](descr = "the bodies of library methods are loaded", default = Some(false), noshort = true)

    // CallGraph




    def getCallGraph(choices: Seq[String], defaultValue: Some[String], description: String): ScallopOption[String] = {
        choice(
            choices = choices,
            default = defaultValue,
            descr = description,
            noshort = true
        )
    }

    // Private methods
    private def getPlainScallopOption(name: String, description: String): ScallopOption[String] = {
        opt[String](name = name, descr = description, noshort = true)
    }

    private def getChoiceScallopOption(name: String, choices: Seq[String], defaultValue: Some[String], description: String): ScallopOption[String] = {
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