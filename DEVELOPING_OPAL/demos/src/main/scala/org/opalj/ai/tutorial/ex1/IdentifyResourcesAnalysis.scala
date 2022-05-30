/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ai.tutorial.ex1

import java.net.URL
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br._
import org.opalj.br.analyses._
import org.opalj.br.instructions._
import org.opalj.ai._

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * @author Michael Eichberg
 */
object IdentifyResourcesAnalysis extends ProjectAnalysisApplication {

    class AnalysisDomain(
            override val project: Project[URL],
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

    override def title: String = "Creation of Resources Using Constant Strings"

    override def description: String =
        "Identifies java.io.File object instantiations using constant strings."

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        // Step 1
        // Find all methods that create "java.io.File(<String>)" objects.
        val callSites = time {
            (for {
                cf <- theProject.allClassFiles.par
                m <- cf.methodsWithBody
            } yield {
                val pcs =
                    m.body.get.foldLeft(List.empty[Int /*PC*/ ]) { (pcs, pc, instruction) =>
                        instruction match {
                            case INVOKESPECIAL(
                                ObjectType("java/io/File"), false /* = isInterface*/ ,
                                "<init>",
                                SingleArgumentMethodDescriptor((ObjectType.String, VoidType))
                                ) =>
                                pc :: pcs
                            case _ =>
                                pcs
                        }
                    }
                (m, pcs)
            }).filter(_._2.size > 0)
        } { ns => println(s"Finding candidates took: ${ns.toSeconds}") }

        // Step 2
        // Perform a simple abstract interpretation to check if there is some
        // method that pass a constant string to a method
        val callSitesWithConstantStringParameter = time {
            for {
                (m, pcs) <- callSites
                result = BaseAI(m, new AnalysisDomain(theProject, m))
                (pc, value) <- pcs.map(pc => (pc, result.operandsArray(pc))).collect {
                    case (pc, result.domain.StringValue(value) :: _) => (pc, value)
                }
            } yield (m, pc, value)
        } { ns => println(s"Performing the abstract interpretations took ${ns.toSeconds}") }

        def callSiteToString(callSite: (Method, Int /* PC*/ , String)): String = {
            val (m, pc, v) = callSite
            m.toJava(s"$pc: $v")
        }

        BasicReport(
            if (callSitesWithConstantStringParameter.isEmpty)
                "Only found "+callSites.size+" candidates."
            else
                callSitesWithConstantStringParameter.map(callSiteToString).
                    mkString("Methods:\n", "\n", ".\n")
        )
    }

}
