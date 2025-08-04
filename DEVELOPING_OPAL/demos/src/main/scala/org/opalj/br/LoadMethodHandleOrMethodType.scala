/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.io.File
import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.LoadMethodHandle
import org.opalj.br.instructions.LoadMethodHandle_W
import org.opalj.br.instructions.LoadMethodType
import org.opalj.br.instructions.LoadMethodType_W

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * @author Michael Eichberg
 */
object LoadMethodHandleOrMethodType extends ProjectsAnalysisApplication {

    protected class MethodHandleStatisticsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects information about loads of method handles and types"
    }

    protected type ConfigType = MethodHandleStatisticsConfig

    protected def createConfig(args: Array[String]): MethodHandleStatisticsConfig =
        new MethodHandleStatisticsConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: MethodHandleStatisticsConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val loads =
            for {
                classFile <- project.allProjectClassFiles.par
                method <- classFile.methodsWithBody
                pcAndInstruction <- method.body.get collect ({
                    case LoadMethodHandle(mh)   => mh
                    case LoadMethodHandle_W(mh) => mh
                    case LoadMethodType(md)     => md
                    case LoadMethodType_W(md)   => md
                }: PartialFunction[Instruction, ConstantValue[?]])
            } yield {
                val pc = pcAndInstruction.pc
                val instruction = pcAndInstruction.value
                method.toJava(s"pc=$pc;load constant=${instruction.valueToString}") +
                    s"<${project.source(classFile.thisType).map(_.toString()).getOrElse("N/A")}>"
            }

        (project, BasicReport(loads.seq.mkString("Instances of LoadMethod(Type|Handle):\n\t", "\n\t", "\n")))
    }
}
