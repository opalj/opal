/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.io.File
import java.net.URL

import org.opalj.br.ClassHierarchy
import org.opalj.br.ClassType
import org.opalj.br.Method
import org.opalj.br.MethodWithBody
import org.opalj.br.PC
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.br.instructions.AASTORE
import org.opalj.br.instructions.ARETURN
import org.opalj.br.instructions.ATHROW
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.MethodInvocationInstruction
import org.opalj.log.OPALLogger

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * Analyses the usage of exceptions in a program.
 *
 * @author Michael Eichberg
 */
object ExceptionUsage extends ProjectsAnalysisApplication {

    protected class ExceptionUsageConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects intra-procedural usage of exceptions"
    }

    protected type ConfigType = ExceptionUsageConfig

    protected def createConfig(args: Array[String]): ExceptionUsageConfig = new ExceptionUsageConfig(args)

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ExceptionUsageConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        implicit val ch: ClassHierarchy = project.classHierarchy

        if (project.classFile(ClassType.Object).isEmpty) {
            OPALLogger.error(
                "analysis configuration",
                "It seems as if the JDK was not loaded; the results of the analysis might not be useful."
            )(using project.logContext)
        }

        val usages = (for {
            classFile <- project.allProjectClassFiles.par
            case method @ MethodWithBody(body) <- classFile.methods
            result = BaseAI(method, new ExceptionUsageAnalysisDomain(project, method))
        } yield {
            import scala.collection.mutable.*

            def typeName(value: result.domain.DomainSingleOriginReferenceValue): String =
                value.upperTypeBound.map(_.toJava).mkString(" with ")

            val exceptionUsages = Map.empty[(PC, String), Set[UsageKind.Value]]

            // 1. let's collect _all_ definition sites (program counters)
            //    of all exceptions (objects that are an instance of Throwable)
            //    (Since exceptions are never created in a register, it is sufficient
            //    to analyze the operands.)
            def collectExceptions(value: result.domain.DomainSingleOriginReferenceValue): Unit = {
                if (value.isNull.isNoOrUnknown &&
                    value.isValueASubtypeOf(ClassType.Throwable).isYes
                ) {
                    val key = (value.origin, typeName(value))
                    if (!exceptionUsages.contains(key)) {
                        exceptionUsages += ((key, Set.empty))
                    }
                }
            }

            for {
                operands <- result.operandsArray;
                if (operands ne null)
                operand <- operands
            } {
                operand match {
                    case v: result.domain.SingleOriginReferenceValue =>
                        collectExceptions(v)
                    case result.domain.MultipleReferenceValues(singleOriginReferenceValues) =>
                        singleOriginReferenceValues.foreach(collectExceptions)
                    case _ => /*not relevant*/
                }
            }

            def updateUsageKindForValue(
                value:     result.domain.DomainSingleOriginReferenceValue,
                usageKind: UsageKind.Value
            ): Unit = {
                exceptionUsages.get((value.origin, typeName(value))).map(_ += usageKind)
            }

            def updateUsageKind(
                value:     result.domain.DomainValue,
                usageKind: UsageKind.Value
            ): Unit = {
                value match {
                    case v: result.domain.SingleOriginReferenceValue =>
                        updateUsageKindForValue(v, usageKind)
                    case result.domain.MultipleReferenceValues(singleOriginReferenceValues) =>
                        singleOriginReferenceValues.foreach { v => updateUsageKindForValue(v, usageKind) }
                    case _ => /*not relevant*/
                }
            }

            body.iterate { (pc, instruction) =>
                val operands = result.operandsArray(pc)
                if (operands != null) { // the instruction is reached...
                    instruction match {
                        case ATHROW =>
                            updateUsageKind(operands.head, UsageKind.IsThrown)
                        case i: MethodInvocationInstruction =>
                            val methodDescriptor = i.methodDescriptor
                            val parametersCount = methodDescriptor.parametersCount
                            operands.take(parametersCount).foreach(updateUsageKind(_, UsageKind.UsedAsParameter))
                            if (i.isVirtualMethodCall) {
                                updateUsageKind(operands.drop(parametersCount).head, UsageKind.UsedAsReceiver)
                            }
                        case _: FieldWriteAccess =>
                            updateUsageKind(operands.head, UsageKind.StoredInField)
                        case ARETURN =>
                            updateUsageKind(operands.head, UsageKind.IsReturned)
                        case AASTORE =>
                            updateUsageKind(operands.head, UsageKind.StoredInArray)
                        case _ =>
                        /*nothing to do*/
                    }
                }
            }

            val usages =
                for {
                    ((pc, typeName), exceptionUsage) <- exceptionUsages
                } yield ExceptionUsage(method, pc, typeName, exceptionUsage)

            if (usages.isEmpty)
                None
            else {
                Some(usages)
            }
        }).filter(_.isDefined).flatMap(_.get)

        val (notUsed, used) = usages.toSeq.partition(_.usageInformation.isEmpty)
        var report = used.map(_.toString).toList.sorted.mkString("\nUsed\n", "\n", "\n")
        report += notUsed.map(_.toString).toList.sorted.mkString("\nNot Used\n", "\n", "\n")

        (project, BasicReport(report))
    }

}

case class ExceptionUsage(
    method:           Method,
    definitionSite:   PC,
    exceptionType:    String,
    usageInformation: scala.collection.Set[UsageKind.Value]
) extends scala.math.Ordered[ExceptionUsage] {

    override def toString: String = {
        val lineNumber = method.body.get.lineNumber(definitionSite).map("line=" + _ + ";").getOrElse("")
        import Console.*
        method.toJava(
            usageInformation.mkString(s"$BOLD[$lineNumber;pc=$definitionSite]$exceptionType => ", ", ", RESET)
        )
    }

    def compare(that: ExceptionUsage): Int = this.toString.compare(that.toString)
}

object UsageKind extends Enumeration {
    val UsedAsParameter = UsageKind.Value
    val UsedAsReceiver = UsageKind.Value
    val IsReturned = UsageKind.Value
    val IsThrown = UsageKind.Value
    val StoredInArray = UsageKind.Value
    val StoredInField = UsageKind.Value
}

class ExceptionUsageAnalysisDomain(val project: SomeProject, val method: Method)
    extends CorrelationalDomain
    with domain.DefaultSpecialDomainValuesBinding
    with domain.l0.TypeLevelPrimitiveValuesConversions
    with domain.l0.DefaultTypeLevelFloatValues
    with domain.l0.DefaultTypeLevelDoubleValues
    with domain.l0.DefaultTypeLevelLongValues
    with domain.l0.TypeLevelLongValuesShiftOperators
    with domain.l0.DefaultTypeLevelIntegerValues
    with domain.l0.TypeLevelFieldAccessInstructions
    with domain.l0.TypeLevelInvokeInstructions
    with domain.l0.TypeLevelDynamicLoads
    with domain.l1.DefaultReferenceValuesBinding
    with domain.DefaultHandlingOfMethodResults
    with domain.IgnoreSynchronization
    with domain.TheProject
    with domain.TheMethod {

    def abortProcessingExceptionsOfCalledMethodsOnUnknownException: Boolean = false
    def abortProcessingThrownExceptionsOnUnknownException: Boolean = false

    def throwExceptionsOnMethodCall: ExceptionsRaisedByCalledMethods.Value = {
        ExceptionsRaisedByCalledMethods.Any
    }

    def throwArithmeticExceptions: Boolean = false

    def throwNullPointerExceptionOnMethodCall: Boolean = false
    def throwNullPointerExceptionOnFieldAccess: Boolean = false
    def throwNullPointerExceptionOnMonitorAccess: Boolean = false
    def throwNullPointerExceptionOnArrayAccess: Boolean = false
    def throwNullPointerExceptionOnThrow: Boolean = false

    def throwIllegalMonitorStateException: Boolean = false

    def throwArrayIndexOutOfBoundsException: Boolean = false
    def throwArrayStoreException: Boolean = false
    def throwNegativeArraySizeException: Boolean = false

    def throwClassCastException: Boolean = false

    def throwClassNotFoundException: Boolean = false
}
