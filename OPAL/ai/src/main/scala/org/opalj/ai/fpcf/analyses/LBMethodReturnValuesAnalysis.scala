/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package analyses

import org.opalj.fpcf.EOptionPSet
import org.opalj.fpcf.LBProperties
import org.opalj.fpcf.Result
import org.opalj.fpcf.PropertiesBoundType
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.SomeEPS
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.ai.domain
import org.opalj.ai.fpcf.domain.RefinedTypeLevelFieldAccessInstructions
import org.opalj.ai.fpcf.domain.RefinedTypeLevelInvokeInstructions
import org.opalj.ai.fpcf.properties.MethodReturnValue
import org.opalj.ai.fpcf.properties.TheMethodReturnValue

/**
 * Computes for each method, which returns object values, general information about the potentially
 * returned values.
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
            val ai:        InterruptableAI[MethodReturnValuesAnalysisDomain],
            val method:    Method,
            val dependees: EOptionPSet[Entity, Property]
    ) extends CorrelationalDomain
        with domain.TheProject
        with domain.TheMethod
        with domain.DefaultSpecialDomainValuesBinding
        with domain.ThrowAllPotentialExceptionsConfiguration
        with domain.l1.DefaultIntegerValues // to enable constant tracking
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
        with domain.IgnoreSynchronization
        with RefinedTypeLevelFieldAccessInstructions
        with RefinedTypeLevelInvokeInstructions {

        def usedPropertiesBound: PropertiesBoundType = LBProperties

        override implicit val project: SomeProject = analysis.project

        override protected[this] def doRecordReturnedValue(pc: PC, value: Value): Boolean = {
            val isUpdated = super.doRecordReturnedValue(pc, value)

            // The idea is to check if the computed return value can no longer be more
            // precise than the "pure" type information. If this is the case, we simply
            // abort the AI
            if (!isUpdated)
                return false;

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
            true // <= the information about the returned value was updated
        }

        // TODO determine/record the accessed fields

    }

    private[analyses] def analyze(method: Method): PropertyComputationResult = {
        val ai = new InterruptableAI[MethodReturnValuesAnalysisDomain]()
        analyze(ai, method, EOptionPSet.empty)
    }

    private[analyses] def analyze(
        ai:        InterruptableAI[MethodReturnValuesAnalysisDomain],
        method:    Method,
        dependees: EOptionPSet[Entity, Property]
    ): ProperPropertyComputationResult = {
        dependees.updateAll() // doesn't hurt if dependees is emnpty
        val domain = new MethodReturnValuesAnalysisDomain(ai, method, dependees)

        val aiResult = ai(method, domain) // the state is implicitly accumulated in the domain

        if (!aiResult.wasAborted) {
            val vi = aiResult.domain.returnedValue.map(_.toCanonicalForm)
            if (domain.dependees.isEmpty
                || vi.isEmpty // <=> the method always ends with an exception
                || vi.get.asReferenceValue.isNull.isYes
                || vi.get.asReferenceValue.isPrecise) {
                Result(method, MethodReturnValue(vi))
            } else {
                // We have potentially relevant dependencies (please, recall that we are currently
                // not flow-sensitive).
                def c(eps: SomeEPS): ProperPropertyComputationResult = {
                    analyze(ai, method, dependees)
                }
                InterimResult.forLB(method, MethodReturnValue(vi), domain.dependees, c)
            }
        } else {
            val mrv = TheMethodReturnValue(ValueInformation.forProperValue(method.returnType))
            Result(FinalEP(method, mrv))
        }
    }

}

object EagerLBMethodReturnValuesAnalysis extends BasicFPCFEagerAnalysisScheduler {

    override def uses: Set[PropertyBounds] = Set.empty

    def derivedProperty: PropertyBounds = PropertyBounds.lb(MethodReturnValue.key)

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new LBMethodReturnValuesAnalysis(p)
        val methods = p.allMethodsWithBody.iterator.filter(m ⇒ m.returnType.isObjectType)
        ps.scheduleEagerComputationsForEntities(methods)(analysis.analyze)
        analysis
    }
}
