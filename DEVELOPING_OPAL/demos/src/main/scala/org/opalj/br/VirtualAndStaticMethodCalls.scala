/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL

import org.opalj.br.instructions._
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Nanoseconds
import org.opalj.br.analyses.ProjectAnalysisApplication

/**
 * Counts the number of static and virtual method calls.
 *
 * @author Michael Eichberg
 */
object VirtualAndStaticMethodCalls extends ProjectAnalysisApplication {

    override def description: String = "Counts the number of static and virtual method calls."

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        var staticCalls = 0
        var virtualCalls = 0
        var executionTime = Nanoseconds.None
        time {
            for {
                classFile <- project.allClassFiles
                method <- classFile.methods
                code <- method.body
                instruction <- code.instructions.collect { case mii: MethodInvocationInstruction => mii }
            } {
                if (instruction.isVirtualMethodCall)
                    virtualCalls += 1
                else
                    staticCalls += 1
            }
        } { t => executionTime = t }

        BasicReport(
            "The sequential analysis took: "+executionTime.toSeconds+"\n"+
                "\tNumber of invokestatic/invokespecial instructions: "+staticCalls+"\n"+
                "\tNumber of invokeinterface/invokevirtual instructions: "+virtualCalls
        )
    }
}
