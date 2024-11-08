package org.opalj.commandlinebase

object AnalysisCommand {
    var name = "analysis"

    private object AnalysisLevelCommand extends OpalChoiceCommand {
        override var name: String = "analysis level"
        override var argName: String = "level"
        override var description: String = "<L0|L1|L2> (Default: L2, the most precise analysis configuration)"
        override var defaultValue: Option[String] = Some("L2")
        override var noshort: Boolean = true
        override var choices: Seq[String] = Seq("L0", "L1", "L2")

        override def parse[T](arg: T): Any = null
    }

    private object RunnerCommand extends OpalPlainCommand[String] {
        override var name: String = "runner name"
        override var argName: String = "runner"
        override var description: String = "The name of the runner, for which some analyses should be set up"
        override var defaultValue: Option[String] = None
        override var noshort: Boolean = true

        override def parse[T](arg: T): Any = null
    }

    def getRunnerCommand(): OpalPlainCommand[String] = RunnerCommand

    def getAnalysisLevelCommand(): OpalChoiceCommand = AnalysisLevelCommand
}
