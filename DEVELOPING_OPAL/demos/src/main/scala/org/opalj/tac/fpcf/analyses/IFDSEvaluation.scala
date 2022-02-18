/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses

import java.io.File

import org.opalj.tac.fpcf.analyses.heros.analyses.taint.HerosBackwardClassForNameAnalysisRunner
import org.opalj.tac.fpcf.analyses.heros.analyses.taint.HerosForwardClassForNameAnalysisRunner
import org.opalj.tac.fpcf.analyses.heros.analyses.HerosAnalysis
import org.opalj.tac.fpcf.analyses.heros.analyses.HerosVariableTypeAnalysisRunner
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.IFDSBasedVariableTypeAnalysis
import org.opalj.tac.fpcf.analyses.ifds.IFDSBasedVariableTypeAnalysisRunner
import org.opalj.tac.fpcf.analyses.taint.BackwardClassForNameTaintAnalysisRunner
import org.opalj.tac.fpcf.analyses.taint.ForwardClassForNameAnalysisRunner

/**
 * Generates some evaluation files related to the AbstractIFDSAnalysis.
 *
 * @author Mario Trageser
 */
object IFDSEvaluation {

    /**
     * args should contain exactly one parameter:
     * A directory, which will contain the evaluation files; terminated with a '/'.
     *
     */
    def main(args: Array[String]): Unit = {
        val dir = args(0)
        new File(dir).mkdirs

        // Evaluation of AbstractIFDSAnalysis
        ForwardClassForNameAnalysisRunner.main(Array("-seq", "-l2", "-f", dir+"ForwardClassForNameAnalysis.txt"))
        BackwardClassForNameTaintAnalysisRunner.main(Array("-seq", "-l2", "-f", dir+"BackwardClassForNameAnalysis.txt"))
        IFDSBasedVariableTypeAnalysisRunner.main(Array("-seq", "-l2", "-f", dir+"IFDSBasedVariableTypeAnalysis.txt"))

        // Evaluation of cross product split in returnFlow
        AbstractIFDSAnalysis.OPTIMIZE_CROSS_PRODUCT_IN_RETURN_FLOW = false
        ForwardClassForNameAnalysisRunner.main(Array("-seq", "-l2", "-f", dir+"ForwardClassForNameAnalysisWithFullCrossProduct.txt"))
        BackwardClassForNameTaintAnalysisRunner.main(Array("-seq", "-l2", "-f", dir+"BackwardClassForNameAnalysisWithFullCrossProduct.txt"))
        AbstractIFDSAnalysis.OPTIMIZE_CROSS_PRODUCT_IN_RETURN_FLOW = true

        // Evaluation of subsuming
        IFDSBasedVariableTypeAnalysis.SUBSUMING = false
        IFDSBasedVariableTypeAnalysisRunner.main(Array("-seq", "-l2", "-f", dir+"NoSubsuming.txt"))
        IFDSBasedVariableTypeAnalysis.SUBSUMING = true

        // Evaluation of Heros analyses
        HerosForwardClassForNameAnalysisRunner.main(Array("-f", dir+"HerosForwardClassForNameAnalysis.txt"))
        HerosBackwardClassForNameAnalysisRunner.main(Array("-f", dir+"HerosBackwardClassForNameAnalysis.txt"))
        HerosVariableTypeAnalysisRunner.main(Array("-f", dir+"HerosVariableTypeAnalysis.txt"))

        // Evaluation of parallel Heros analyses
        HerosAnalysis.NUM_THREADS = 6
        HerosForwardClassForNameAnalysisRunner.main(Array("-f", dir+"ParallelHerosForwardClassForNameAnalysis.txt"))
        HerosBackwardClassForNameAnalysisRunner.main(Array("-f", dir+"ParallelHerosBackwardClassForNameAnalysis.txt"))
        HerosVariableTypeAnalysisRunner.main(Array("-f", dir+"ParallelHerosVariableTypeAnalysis.txt"))
        HerosAnalysis.NUM_THREADS = 1

        // Evaluation of the scheduling strategies
        IFDSBasedVariableTypeAnalysis.SUBSUMING = false
        ForwardClassForNameAnalysisRunner.main(Array("-evalSchedulingStrategies", "-seq", "-l2", "-f", dir+"SchedulingForwardClassForNameAnalysis.txt"))
        BackwardClassForNameTaintAnalysisRunner.main(Array("-evalSchedulingStrategies", "-seq", "-l2", "-f", dir+"SchedulingBackwardClassForNameAnalysis.txt"))
        IFDSBasedVariableTypeAnalysisRunner.main(Array("-evalSchedulingStrategies", "-seq", "-l2", "-f", dir+"SchedulingIFDSBasedVariableTypeAnalysis.txt"))
        IFDSBasedVariableTypeAnalysis.SUBSUMING = true
    }

}
