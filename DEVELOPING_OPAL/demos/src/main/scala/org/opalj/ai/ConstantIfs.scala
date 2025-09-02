/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters.*

import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.instructions.IFICMPInstruction

/**
 * A shallow analysis that tries to identify useless computations; here, "ifs" where the condition is constant.
 *
 * @author Michael Eichberg
 */
object ConstantIfs extends ProjectsAnalysisApplication {

    protected class ConstantIfsConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects ifs where the condition is constant"
    }

    protected type ConfigType = ConstantIfsConfig

    protected def createConfig(args: Array[String]): ConstantIfsConfig = new ConstantIfsConfig(args)

    class AnalysisDomain(val project: SomeProject, val method: Method)
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

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ConstantIfsConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val results = new ConcurrentLinkedQueue[String]()
        project.parForeachMethodWithBody() { m =>
            val method = m.method
            val result = BaseAI(method, new AnalysisDomain(project, method))
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

        (project, BasicReport(results.asScala.mkString(s"${results.size} useless computations:\n", "\n", "\n")))
    }
}
