/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.io.File

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.instructions._
import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time

/**
 * Counts the number of static and virtual method calls.
 *
 * @author Michael Eichberg
 */
object VirtualAndStaticMethodCalls extends ProjectsAnalysisApplication {

    protected class MethodCallsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Counts the number of static and virtual method calls"
    }

    protected type ConfigType = MethodCallsConfig

    protected def createConfig(args: Array[String]): MethodCallsConfig = new MethodCallsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: MethodCallsConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

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

        (
            project,
            BasicReport(
                "The sequential analysis took: " + executionTime.toSeconds + "\n" +
                    "\tNumber of invokestatic/invokespecial instructions: " + staticCalls + "\n" +
                    "\tNumber of invokeinterface/invokevirtual instructions: " + virtualCalls
            )
        )
    }
}
