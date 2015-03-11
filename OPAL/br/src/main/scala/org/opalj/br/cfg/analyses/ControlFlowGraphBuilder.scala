package org.opalj.br.cfg.analyses

import java.net.URL
import org.opalj.bytecode.BytecodeProcessingFailedException
import org.opalj.br.cfg._
import org.opalj.util.PerformanceEvaluation.ns2sec
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
//import org.opalj.br.TestSupport

object ControlFlowGraphBuilder
        extends AnalysisExecutor
        with OneStepAnalysis[URL, BasicReport] {

    val analysis = this

    def doAnalyze(
        theProject: Project[URL],
        parameters: Seq[String],
        isInterrupted: () ⇒ Boolean) = {

        var finalMessage: String = "Building of CFGs successful!"

        time {
            theProject.parForeachMethodWithBody[Unit](isInterrupted) { param ⇒
                val (_, classfile, method) = param

                try {
                    ControlFlowGraph(method)
                } catch {
                    case bpf: BytecodeProcessingFailedException ⇒ {}
                    case e: Exception ⇒ {
                        finalMessage = "Exception of the type "+e.getClass().getName()+" occured in class file "+classfile.fqn+" in method "+method.name
                        sys.exit
                    }
                }
            }

        } { t ⇒ println("Creating all controlf flow graphs took: "+ns2sec(t)) }

        BasicReport(finalMessage)
    }
}