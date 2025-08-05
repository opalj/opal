/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.io.File
import java.net.URL

import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig

/**
 * Computes some statistics related to the number of parameters and locals
 * (Local Variables/Registers) defined/specified by each method.
 *
 * @author Michael Eichberg
 */
object MaxLocalsEvaluation extends ProjectsAnalysisApplication {

    protected class MaxLocalsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Computes information about the maximum number of registers required per method"
    }

    protected type ConfigType = MaxLocalsConfig

    protected def createConfig(args: Array[String]): MaxLocalsConfig = new MaxLocalsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: MaxLocalsConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        import scala.collection.immutable.TreeMap // <= Sorted...
        var methodParametersDistribution: Map[Int, Int] = TreeMap.empty
        var maxLocalsDistrbution: Map[Int, Int] = TreeMap.empty

        val (project, _) = analysisConfig.setupProject(cp)

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

        (
            project,
            BasicReport("\nResults:\n" +
                "Method Parameters Distribution:\n" +
                "#Parameters\tFrequency:\n" +
                methodParametersDistribution.map(kv => { val (k, v) = kv; s"$k\t\t$v" }).mkString("\n") + "\n\n" +
                "MaxLocals Distribution:\n" +
                "#Locals\t\tFrequency:\n" +
                maxLocalsDistrbution.map(kv => { val (k, v) = kv; s"$k\t\t$v" }).mkString("\n"))
        )
    }
}
