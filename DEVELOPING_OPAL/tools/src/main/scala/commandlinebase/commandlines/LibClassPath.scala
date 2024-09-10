package commandlinebase.commandlines

import commandlinebase.OpalCommandLine
import org.opalj.support.info.OpalConfBase
import org.rogach.scallop.ScallopOption


class LibClassPath (opalConfBase: OpalConfBase) extends OpalCommandLine[String, Some[String]] {

    override val commandLine: ScallopOption[String] = opalConfBase.getLibClassPath()

    override def parse(): Some[String] = {
        new Some(commandLine.apply())
    }
}
