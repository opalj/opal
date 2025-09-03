/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import scala.language.postfixOps

import java.io.File
import java.net.URL

import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.br.ClassType
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.cli.ClassNameArg
import org.opalj.cli.PartialSignatureArg

/**
 * Collects points-to information related to a method.
 *
 * @author Michael Eichberg
 */
object LocalPointsTo extends ProjectsAnalysisApplication {

    protected class LocalPointsToConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description =
            "Collects points-to information related to a method"

        args(
            ClassNameArg !,
            PartialSignatureArg !
        )
    }

    protected type ConfigType = LocalPointsToConfig

    protected def createConfig(args: Array[String]): LocalPointsToConfig = new LocalPointsToConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: LocalPointsToConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val result = new StringBuilder()

        for {
            className <- analysisConfig(ClassNameArg)
            cf <- project.classFile(ClassType(className.replace('.', '/')))
            m <- cf.methods
            if analysisConfig(PartialSignatureArg).exists(sig => m.signatureToJava().contains(sig._2 + sig._3))
        } {

            // ... perform the data-flow analysis
            val aiResult = BaseAI(m, new PrimitiveTACAIDomain(project, m))
            // now, we can transform the bytecode to three-address code
            val tac = TACAI(m, project.classHierarchy, aiResult, propagateConstants = true)(Nil /* no optimizations */ )

            // Let's print the three address code to get a better feeling for it...
            // Please note, "pc" in the output refers to the program counter of the original
            // underlying bytecode instruction; the "pc" is kept to avoid that we have to
            // transform the other information in the class file too.
            result.append(tac)
            result.append('\n')

            // Let's collect the information where a reference value that is passed
            // to some method is coming from.
            for {
                (MethodCallParameters(params), stmtIndex) <- tac.stmts.iterator.zipWithIndex
                (UVar(v, defSites), paramIndex) <- params.iterator.zipWithIndex
                if v.computationalType == ComputationalTypeReference
                defSite <- defSites
            } {
                if (defSite >= 0) {
                    val Assignment(_, _, expr) = tac.stmts(defSite) // a def site is always an assignment
                    result.append(s"call@$stmtIndex(param=$paramIndex) is " + expr + "\n")
                } else {
                    result.append(s"call@$stmtIndex(param=$paramIndex) takes param " + (-defSite - 1) + "\n")
                }
            }
        }

        (project, BasicReport(result.toString()))
    }
}
