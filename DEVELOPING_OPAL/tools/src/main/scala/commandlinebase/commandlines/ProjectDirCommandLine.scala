package commandlinebase.commandlines

import commandlinebase.OpalCommandLine
import org.opalj.support.info.OpalConfBase
import org.rogach.scallop.ScallopOption

class ProjectDirCommandLine(opalConfBase: OpalConfBase) extends OpalCommandLine[String, Some[String]] {
    override val commandLine: ScallopOption[String] = opalConfBase.getProjectDir()

    override def parse(): Some[String] = {
        Some(commandLine.apply())
    }
}
