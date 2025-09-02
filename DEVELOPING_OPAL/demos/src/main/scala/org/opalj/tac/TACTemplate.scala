/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.io.File

import org.opalj.ai.cli.AIBasedCommandLineConfig
import org.opalj.br.ClassType
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.cli.ClassNameArg
import org.opalj.cli.PartialSignatureArg

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * A template for implementing 3-address code based analyses.
 *
 * @author Michael Eichberg
 */
object TACTemplate extends ProjectsAnalysisApplication {

    protected class TACConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args)
        with AIBasedCommandLineConfig {
        val description = "A template for implementing 3-address code based analyses"

        // 1.
        // Declaration of analysis specific parameters
        args(
            ClassNameArg,
            PartialSignatureArg
        )
    }

    protected type ConfigType = TACConfig

    protected def createConfig(args: Array[String]): TACConfig = new TACConfig(args)

    override protected def evaluate(
        cp:             Iterable[File],
        analysisConfig: TACConfig,
        execution:      Int
    ): Unit = {
        // 2.
        // Interpreting command line parameters
        val className = analysisConfig.get(ClassNameArg)
        val methodSignature = analysisConfig.get(PartialSignatureArg)

        // 3.
        // Instantiate the Project
        val (project, _) = analysisConfig.setupProject(cp)

        // 4.
        // Get the TACAI key
        val tac = project.get(LazyTACUsingAIKey)

        // 5.
        // Perform analysis
        // As part of this template, we just demonstrate how to print the virtual methods
        // calls of a selected set of methods.
        for {
            cf <- project.allProjectClassFiles.par // OPAL is generally, thread safe and facilitates parallelization
            if className.isEmpty || className.get.exists(cf.thisType.toJava.contains)
            m <- cf.methods
            if m.body.isDefined
            if methodSignature.isEmpty || methodSignature.get.exists(s => m.signature.toJava.contains(s._2 + s._3))
            c = tac(m)
            case VirtualFunctionCallStatement(VirtualFunctionCall(
                pc,
                declaringClass: ClassType,
                _,
                name,
                descriptor,
                receiver,
                _
            )) <- c.stmts
        } {
            println(
                m.toJava(s"$pc: virtual function call of $receiver.${descriptor.toJava(declaringClass.toJava, name)}")
            )
        }

        println("Done.")
    }
}
