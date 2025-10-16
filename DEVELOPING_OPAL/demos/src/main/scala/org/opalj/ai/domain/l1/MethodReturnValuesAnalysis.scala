/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import java.io.File
import java.net.URL

import org.opalj.ai.CorrelationalDomain
import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.value.TypeOfReferenceValue

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * A shallow analysis that tries to refine the return types of methods.
 *
 * @author Michael Eichberg
 */
object MethodReturnValuesAnalysis extends ProjectsAnalysisApplication {

    protected class MethodReturnValueConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description =
            "Identifies methods where we can – statically – derive more precise return type/value information"
    }

    protected type ConfigType = MethodReturnValueConfig

    protected def createConfig(args: Array[String]): MethodReturnValueConfig = new MethodReturnValueConfig(args)

    class AnalysisDomain(
        override val project: SomeProject,
        val ai:               InterruptableAI[?],
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
        with domain.l1.DefaultReferenceValuesBinding
        with domain.DefaultHandlingOfMethodResults
        with domain.IgnoreSynchronization
        with domain.TheProject
        with domain.TheMethod
        with domain.RecordReturnedValuesInfrastructure {

        type ReturnedValue = DomainValue

        private val originalReturnType: ReferenceType =
            method.descriptor.returnType.asReferenceType

        private var theReturnedValue: DomainValue = null

        // e.g., a method that always throws an exception...
        def returnedValue: Option[DomainValue] = Option(theReturnedValue)

        protected def doRecordReturnedValue(pc: Int, value: DomainValue): Boolean = {
            val isUpdated =
                if (theReturnedValue == null) {
                    theReturnedValue = value.summarize(Int.MinValue)
                    true
                } else {
                    val newReturnedValue = summarize(Int.MinValue, Iterable(theReturnedValue, value))
                    if (newReturnedValue ne theReturnedValue) {
                        theReturnedValue = newReturnedValue
                        true
                    } else {
                        false
                    }
                }

            // Test if it make sense to continue the abstract interpretation or if the
            // return value information is already not more precise than the "return type".
            theReturnedValue match {
                case rv @ TypeOfReferenceValue(UIDSet1(`originalReturnType`))
                    if (
                        rv.isNull.isUnknown && !rv.isPrecise
                    ) =>
                    // the return type will not be more precise than the original type
                    ai.interrupt()

                case _ => /*go on*/
            }

            isUpdated
        }
    }

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: MethodReturnValueConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject()
        val refinedMethods = time {
            for {
                classFile <- project.allClassFiles.par
                method <- classFile.methods
                if method.body.isDefined
                originalType = method.returnType
                if method.returnType.isReferenceType
                ai = new InterruptableAI[Domain]
                domain = new AnalysisDomain(project, ai, method)
                result = ai(method, domain)
                if !result.wasAborted
                returnedValue = domain.returnedValue
                if returnedValue.isEmpty ||
                    returnedValue.get.asDomainReferenceValue.upperTypeBound != UIDSet(originalType)
            } yield {
                RefinedReturnType(method, domain.returnedValue)
            }
        } { ns => println(s"the analysis took ${ns.toSeconds}") }

        (
            project,
            BasicReport(
                refinedMethods.mkString(
                    "Methods with refined return types (" + refinedMethods.size + "): \n",
                    "\n",
                    "\n"
                )
            )
        )
    }

}

case class RefinedReturnType(method: Method, refinedType: Option[Domain#DomainValue]) {

    override def toString(): String = {
        import Console.*
        "Refined the return type of " + BOLD + BLUE + method.toJava + " => " +
            GREEN + refinedType.getOrElse("\"NONE\" (the method does not return normally)") + RESET
    }

}
