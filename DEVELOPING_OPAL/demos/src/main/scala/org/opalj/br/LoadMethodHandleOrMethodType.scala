/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.LoadMethodHandle
import org.opalj.br.instructions.LoadMethodHandle_W
import org.opalj.br.instructions.LoadMethodType
import org.opalj.br.instructions.LoadMethodType_W

/**
 * @author Michael Eichberg
 */
object LoadMethodHandleOrMethodType extends ProjectAnalysisApplication {

    override def description: String = "prints information about loads of method handles and types"

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

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

        BasicReport(loads.seq.mkString("Instances of LoadMethod(Type|Handle):\n\t", "\n\t", "\n"))
    }
}
