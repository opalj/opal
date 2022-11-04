/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue

import scala.jdk.CollectionConverters._

import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.IFICMPInstruction

/**
 * A shallow analysis that tries to identify useless computations; here, "ifs" where the condition
 * is constant.
 *
 * @author Michael Eichberg
 */
object UselessComputationsMinimal extends ProjectAnalysisApplication {

    class AnalysisDomain(val project: Project[URL], val method: Method)
        extends CorrelationalDomain
        with domain.DefaultSpecialDomainValuesBinding
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l0.TypeLevelDynamicLoads
        with domain.l1.DefaultReferenceValuesBinding
        with domain.l1.DefaultIntegerRangeValues
        with domain.l1.DefaultLongValues
        with domain.l1.ConcretePrimitiveValuesConversions
        with domain.l1.LongValuesShiftOperators
        with domain.TheProject
        with domain.TheMethod

    def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        val results = new ConcurrentLinkedQueue[String]()
        theProject.parForeachMethodWithBody(isInterrupted) { m =>
            val method = m.method
            val result = BaseAI(method, new AnalysisDomain(theProject, method))
            import result.domain.ConcreteIntegerValue
            collectPCWithOperands(result.domain)(method.body.get, result.operandsArray) {
                case (
                    pc,
                    _: IFICMPInstruction[_],
                    Seq(ConcreteIntegerValue(a), ConcreteIntegerValue(b), _*)
                    ) =>
                    val context = method.toJava
                    val result = s"$context: /*pc=$pc:*/ comparison of constant values: $a and $b"
                    results.add(result)
            }
        }

        BasicReport(results.asScala.mkString(s"${results.size} useless computations:\n", "\n", "\n"))
    }
}
