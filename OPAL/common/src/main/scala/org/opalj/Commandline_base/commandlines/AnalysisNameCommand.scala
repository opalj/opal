package org.opalj.Commandline_base.commandlines

import java.util.Calendar

object AnalysisNameCommand extends OpalPlainCommand[String] {
    override var name: String = "analysis name"
    override var argName: String = "analysisName"
    override var description: String = "analysisName which defines the analysis within the results file"
    override var defaultValue: Option[String] = Some(s"RUN-${Calendar.getInstance().getTime.toString}")
    override var noshort: Boolean = true

    override def parse[T](arg: T): Any = null
}
