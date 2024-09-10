package commandlinebase.commandlines

import commandlinebase.OpalCommandLine
import org.opalj.support.info.OpalConfBase
import org.rogach.scallop.ScallopOption

import java.io.File

class ClassPathCommandLine(opalConfBase: OpalConfBase) extends OpalCommandLine[String, File] {

    override val commandLine: ScallopOption[String] = opalConfBase.getClassPath();

    override def parse(): File = {
        new File(commandLine.apply())
    }
}
