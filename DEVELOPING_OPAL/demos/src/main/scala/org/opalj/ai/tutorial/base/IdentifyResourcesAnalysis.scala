/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package tutorial
package base

import java.io.File
import java.net.URL

import org.opalj.br.ClassType
import org.opalj.br.Method
import org.opalj.br.SingleArgumentMethodDescriptor
import org.opalj.br.VoidType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.util.PerformanceEvaluation.time

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * @author Michael Eichberg
 */
object IdentifyResourcesAnalysis extends ProjectsAnalysisApplication {

    protected class ResourcesConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Identifies java.io.File object instantiations using constant strings"
    }

    protected type ConfigType = ResourcesConfig

    protected def createConfig(args: Array[String]): ResourcesConfig = new ResourcesConfig(args)

    class AnalysisDomain(
        override val project: SomeProject,
        val method:           Method
    ) extends CorrelationalDomain
        with domain.DefaultSpecialDomainValuesBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelIntegerValues
        with domain.l0.DefaultTypeLevelLongValues
        with domain.l0.TypeLevelPrimitiveValuesConversions
        with domain.l0.TypeLevelLongValuesShiftOperators
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l0.TypeLevelDynamicLoads
        // NOT NEEDED: with domain.l1.DefaultReferenceValuesBinding
        with domain.l1.DefaultStringValuesBinding
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.TheProject
        with domain.TheMethod

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ResourcesConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        // Step 1
        // Find all methods that create "java.io.File(<String>)" objects.
        val callSites = time {
            (for {
                cf <- project.allClassFiles.par
                m <- cf.methodsWithBody
            } yield {
                val pcs =
                    m.body.get.foldLeft(List.empty[Int /*PC*/ ]) { (pcs, pc, instruction) =>
                        instruction match {
                            case INVOKESPECIAL(
                                    ClassType("java/io/File"),
                                    false /* = isInterface*/,
                                    "<init>",
                                    SingleArgumentMethodDescriptor((ClassType.String, VoidType))
                                ) =>
                                pc :: pcs
                            case _ =>
                                pcs
                        }
                    }
                (m, pcs)
            }).filter(_._2.nonEmpty)
        } { ns => println(s"Finding candidates took: ${ns.toSeconds}") }

        // Step 2
        // Perform a simple abstract interpretation to check if there is some
        // method that pass a constant string to a method
        val callSitesWithConstantStringParameter = time {
            for {
                (m, pcs) <- callSites
                result = BaseAI(m, new AnalysisDomain(project, m))
                (pc, value) <- pcs.map(pc => (pc, result.operandsArray(pc))).collect {
                    case (pc, result.domain.StringValue(value) :: _) => (pc, value)
                }
            } yield (m, pc, value)
        } { ns => println(s"Performing the abstract interpretations took ${ns.toSeconds}") }

        def callSiteToString(callSite: (Method, Int /* PC*/, String)): String = {
            val (m, pc, v) = callSite
            m.toJava(s"$pc: $v")
        }

        val report = BasicReport(
            if (callSitesWithConstantStringParameter.isEmpty)
                "Only found " + callSites.size + " candidates."
            else
                callSitesWithConstantStringParameter.map(callSiteToString).mkString("Methods:\n", "\n", ".\n")
        )
        (project, report)
    }

}
