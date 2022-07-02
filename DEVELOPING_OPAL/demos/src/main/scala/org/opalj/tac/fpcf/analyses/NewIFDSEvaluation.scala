/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses

import java.io.File

import org.opalj.tac.fpcf.analyses.heros.analyses.HerosVariableTypeAnalysisRunner
import org.opalj.tac.fpcf.analyses.ifds.old
import org.opalj.tac.fpcf.analyses.ifds.IFDSBasedVariableTypeAnalysisRunner

/**
 * Generates some evaluation files related to the AbstractIFDSAnalysis.
 *
 * @author Mario Trageser
 */
object NewIFDSEvaluation {

    /**
     * args should contain exactly one parameter:
     * A directory, which will contain the evaluation files; terminated with a '/'.
     *
     */
    def main(args: Array[String]): Unit = {
        val dir = args(0)
        new File(dir).mkdirs

        old.IFDSBasedVariableTypeAnalysisRunner.main(Array("-seq", "-l2", "-f", dir+"OldIFDSBasedVariableTypeAnalysis.txt"))
        HerosVariableTypeAnalysisRunner.main(Array("-f", dir+"HerosVariableTypeAnalysis.txt"))
        IFDSBasedVariableTypeAnalysisRunner.main(Array("-seq", "-l2", "-f", dir+"IFDSBasedVariableTypeAnalysis.txt"))
    }

}
