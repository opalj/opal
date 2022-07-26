/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import java.net.URL
import org.opalj.ai.domain
import org.opalj.value.TypeOfReferenceValue
import org.opalj.br.Method
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.UIDSet1
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.ai.CorrelationalDomain
import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * A shallow analysis that tries to refine the return types of methods.
 *
 * @author Michael Eichberg
 */
object MethodReturnValuesAnalysis extends ProjectAnalysisApplication {

    class AnalysisDomain(
            override val project: Project[java.net.URL],
            val ai:               InterruptableAI[_],
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

        private[this] val originalReturnType: ReferenceType =
            method.descriptor.returnType.asReferenceType

        private[this] var theReturnedValue: DomainValue = null

        // e.g., a method that always throws an exception...
        def returnedValue: Option[DomainValue] = Option(theReturnedValue)

        protected[this] def doRecordReturnedValue(pc: Int, value: DomainValue): Boolean = {
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
                case rv @ TypeOfReferenceValue(UIDSet1(`originalReturnType`)) if (
                    rv.isNull.isUnknown && !rv.isPrecise
                ) =>
                    // the return type will not be more precise than the original type
                    ai.interrupt()

                case _ => /*go on*/
            }

            isUpdated
        }
    }

    override def title: String = "Derives Information About Returned Values"

    override def description: String =
        "Identifies methods where we can – statically – derive more precise return type/value information."

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        val methodsWithRefinedReturnTypes = time {
            for {
                classFile <- theProject.allClassFiles.par
                method <- classFile.methods
                if method.body.isDefined
                originalType = method.returnType
                if method.returnType.isReferenceType
                ai = new InterruptableAI[Domain]
                domain = new AnalysisDomain(theProject, ai, method)
                result = ai(method, domain)
                if !result.wasAborted
                returnedValue = domain.returnedValue
                if returnedValue.isEmpty ||
                    returnedValue.get.asDomainReferenceValue.upperTypeBound != UIDSet(originalType)
            } yield {
                RefinedReturnType(method, domain.returnedValue)
            }
        } { ns => println(s"the analysis took ${ns.toSeconds}") }

        BasicReport(
            methodsWithRefinedReturnTypes.mkString(
                "Methods with refined return types ("+methodsWithRefinedReturnTypes.size+"): \n", "\n", "\n"
            )
        )
    }

}

case class RefinedReturnType(method: Method, refinedType: Option[Domain#DomainValue]) {

    override def toString(): String = {
        import Console._
        "Refined the return type of "+BOLD + BLUE + method.toJava+" => "+
            GREEN + refinedType.getOrElse("\"NONE\" (the method does not return normally)") + RESET
    }

}
