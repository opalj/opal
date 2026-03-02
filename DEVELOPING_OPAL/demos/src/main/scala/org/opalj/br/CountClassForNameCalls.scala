/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.io.File
import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.instructions.INVOKESTATIC

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * Counts the number of `Class.forName` calls.
 *
 * @author Michael Eichberg
 */
object CountClassForNameCalls extends ProjectsAnalysisApplication {

    protected class ClassForNameConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Counts the number of Class.forName calls"
    }

    protected type ConfigType = ClassForNameConfig

    protected def createConfig(args: Array[String]): ClassForNameConfig = new ClassForNameConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ClassForNameConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        import ClassType.Class
        import ClassType.String
        // Next, we create a descriptor of a method that takes a single parameter of
        // type "String" and that returns a value of type Class.
        val descriptor = MethodDescriptor(String, Class)
        val invokes =
            // The following collects all calls of the method "forName" on
            // an object of type "java.lang.Class".
            for {
                // Let's traverse all methods of all class files that have a
                // concrete (non-native) implementation.
                classFile <- project.allProjectClassFiles.par
                case method @ MethodWithBody(code) <- classFile.methods
                // Match all invocations of the method:
                // Class.forName(String) : Class<?>
                case PCAndInstruction(pc, INVOKESTATIC(Class, _, "forName", `descriptor`)) <- code
            } yield {
                method.toJava(s"pc=$pc")
            }
        val header = s"found ${invokes.size} calls of Class.forName(String)\n\t"
        (project, BasicReport(invokes.seq.toList.sorted.mkString(header, "\n\t", "\n")))
    }

}
