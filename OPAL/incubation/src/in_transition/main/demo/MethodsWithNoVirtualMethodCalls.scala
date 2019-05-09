/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.instructions.VirtualMethodInvocationInstruction
import org.opalj.ai.analyses.{MethodReturnValuesAnalysis ⇒ TheAnalysis}

/**
 * A shallow analysis that identifies methods that do not perform virtual method
 * calls.
 *
 * @author Michael Eichberg
 */
object MethodsWithNoVirtualMethodCalls extends ProjectAnalysisApplication {

    override def title: String = "identifies methods that perform no virtual method calls"

    override def description: String = TheAnalysis.description

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val allMethods = new AtomicInteger(0)

        val methodsWithoutVirtualMethodCalls =
            time {
                for {
                    classFile ← theProject.allClassFiles.par
                    (method, body) ← classFile.methodsWithBody
                    if { allMethods.incrementAndGet(); true }
                    if body.instructions.exists { i ⇒ i.isInstanceOf[VirtualMethodInvocationInstruction] }
                } yield {
                    method.toJava
                }
            } { t ⇒ println(s"Analysis time: $t") }

        BasicReport(
            methodsWithoutVirtualMethodCalls.map(_.toString()).seq.toSeq.sorted.mkString(
                s"${methodsWithoutVirtualMethodCalls.size} methods out of ${allMethods.get} methods with a body perfom no virtual method call\n",
                "\n",
                "\n"
            )
        )
    }
}
