/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package analyses

import org.opalj.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.fpcf.FPCFAnalysis
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.ai.fpcf.properties.MethodReturnValue

/**
 * Computes for each method which returns a reference value an approximation
 * of the values that are potentially returned.
 *
 * @author Michael Eichberg
 */
class LBMethodReturnValuesAnalysis private[analyses] (
        val project: SomeProject
) extends FPCFAnalysis { analysis ⇒

    /**
     *  A very basic domain that we use for analyzing the values returned by the methods.
     *
     * @author Michael Eichberg
     */
    class MethodReturnValuesAnalysisDomain(
            val ai:     InterruptableAI[MethodReturnValuesAnalysisDomain],
            val method: Method
    ) extends CorrelationalDomain
        with domain.TheProject
        with domain.TheMethod
        with domain.DefaultSpecialDomainValuesBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l0.DefaultTypeLevelIntegerValues
        with domain.l0.DefaultTypeLevelLongValues
        with domain.l0.DefaultTypeLevelFloatValues
        with domain.l0.DefaultTypeLevelDoubleValues
        with domain.l0.TypeLevelPrimitiveValuesConversions
        with domain.l0.TypeLevelLongValuesShiftOperators
        with domain.l0.TypeLevelFieldAccessInstructions
        with domain.l0.TypeLevelInvokeInstructions
        with domain.l1.DefaultReferenceValuesBinding
        with domain.DefaultHandlingOfMethodResults
        with domain.RecordReturnedValue
        with domain.IgnoreSynchronization {

        override implicit val project: SomeProject = analysis.project

        override protected[this] def doRecordReturnedValue(pc: RefId, value: Value): Unit = {
            super.doRecordReturnedValue(pc, value)

            // The idea is to check if the computed return value can no longer be more
            // precise than the "pure" type information. If this is the case, we simply
            // abort the AI
            val returnedReferenceValue = theReturnedValue.asDomainReferenceValue
            if (returnedReferenceValue.isNull.isUnknown &&
                returnedReferenceValue.upperTypeBound.isSingletonSet &&
                returnedReferenceValue.upperTypeBound.head == method.returnType &&
                (!returnedReferenceValue.isPrecise ||
                    // Though the value is precise, no one cares if the type is (effectively) final
                    // or not extensible
                    // TODO Use the information about "ExtensibleTypes" to filter more irrelevant cases
                    classHierarchy.isKnownToBeFinal(returnedReferenceValue.upperTypeBound.head))) {
                ai.interrupt()
            }
        }

        // TODO determine/record the accessed fields

    }

    private[analyses] def analyze(method: Method): PropertyComputationResult = {
        val ai = new InterruptableAI[MethodReturnValuesAnalysisDomain]()
        val domain = new MethodReturnValuesAnalysisDomain(ai, method)
        val aiResult = ai(method, domain) // the state is implicitly accumulated in the domain
        if (!aiResult.wasAborted) {
            Result(method, MethodReturnValue(aiResult.domain.returnedValue.map(_.toCanonicalForm)))
        } else {
            NoResult
        }
    }

}

object EagerLBMethodReturnValuesAnalysis extends BasicFPCFEagerAnalysisScheduler {

    final override def uses: Set[PropertyKind] = Set()

    final override def derives: Set[PropertyKind] = Set(MethodReturnValue.key)

    final override def refinesLowerBound: Boolean = true

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new LBMethodReturnValuesAnalysis(p)
        val methods = p.allMethodsWithBody.iterator.filter(m ⇒ m.returnType.isObjectType)
        ps.scheduleEagerComputationsForEntities(methods)(analysis.analyze)
        analysis
    }
}
