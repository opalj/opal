/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication

/**
 * Computes some statistics related to the number of parameters and locals
 * (Local Variables/Registers) defined/specified by each method.
 *
 * @author Michael Eichberg
 */
object MaxLocalsEvaluation extends ProjectAnalysisApplication {

    override def title: String = "Maximum Number of Locals"

    override def description: String = {
        "provide information about the maximum number of registers required per method"
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        import scala.collection.immutable.TreeMap // <= Sorted...
        var methodParametersDistribution: Map[Int, Int] = TreeMap.empty
        var maxLocalsDistrbution: Map[Int, Int] = TreeMap.empty

        for {
            classFile <- project.allProjectClassFiles
            method @ MethodWithBody(body) <- classFile.methods
            descriptor = method.descriptor
        } {
            val parametersCount = descriptor.parametersCount + (if (method.isStatic) 0 else 1)

            val methodParametersFrequency = methodParametersDistribution.getOrElse(parametersCount, 0) + 1
            methodParametersDistribution =
                methodParametersDistribution.updated(parametersCount, methodParametersFrequency)

            val newMaxLocalsCount = maxLocalsDistrbution.getOrElse(body.maxLocals, 0) + 1
            maxLocalsDistrbution = maxLocalsDistrbution.updated(body.maxLocals, newMaxLocalsCount)
        }

        BasicReport("\nResults:\n"+
            "Method Parameters Distribution:\n"+
            "#Parameters\tFrequency:\n"+
            methodParametersDistribution.map(kv => { val (k, v) = kv; s"$k\t\t$v" }).mkString("\n")+"\n\n"+
            "MaxLocals Distribution:\n"+
            "#Locals\t\tFrequency:\n"+
            maxLocalsDistrbution.map(kv => { val (k, v) = kv; s"$k\t\t$v" }).mkString("\n"))
    }
}
