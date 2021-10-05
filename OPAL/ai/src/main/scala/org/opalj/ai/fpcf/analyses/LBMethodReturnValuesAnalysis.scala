/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package analyses

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionPSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.LBProperties
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SinglePropertiesBoundType
import org.opalj.fpcf.SomeEPS
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.ai.domain
import org.opalj.ai.fpcf.domain.RefinedTypeLevelFieldAccessInstructions
import org.opalj.ai.fpcf.domain.RefinedTypeLevelInvokeInstructions
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.fpcf.properties.MethodReturnValue
import org.opalj.ai.fpcf.properties.TheMethodReturnValue

/**
 * Computes for each method that returns object typed values general information about the
 * potentially returned values.
 *
 * @author Michael Eichberg
 */
class LBMethodReturnValuesAnalysis private[analyses] (
        val project: SomeProject
) extends FPCFAnalysis { analysis =>

    /**
     *  A very basic domain that we use for analyzing the values returned by a method.
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
        with domain.l0.TypeLevelDynamicLoads
        with domain.l1.DefaultReferenceValuesBinding
        with domain.DefaultHandlingOfMethodResults
        with domain.RecordReturnedValue
        with domain.IgnoreSynchronization
        with RefinedTypeLevelFieldAccessInstructions
        with RefinedTypeLevelInvokeInstructions {

        final override val UsedPropertiesBound: SinglePropertiesBoundType = LBProperties

        override implicit val project: SomeProject = analysis.project

        override protected[this] def doRecordReturnedValue(pc: PC, value: Value): Boolean = {
            val isUpdated = super.doRecordReturnedValue(pc, value)

            // The idea is to check if the computed return value can no longer be more
            // precise than the "pure" type information. If this is the case, we simply
            // abort the AI

            val returnedReferenceValue = theReturnedValue.asDomainReferenceValue
            // IN GENERAL, we would like the following assertion to always hold,
            // but – given that basically every code base has loose ends – it
            // may happen that the class hierarchy lacks crucial information to
            // make it possible to compute the correct upper bound. E.g., in case
            // of the JDK, the type hierarchy relation between the used eclipse
            // classes is not known and therefore the common supertype of ...dnd.RTFTextTransfer
            // and dnd.TextTransfer is computed as Object, though it is dnd.Transfer.
            // In this case, it may happen that the returned value (Object in the above case)
            // suddenly becomes "less precise" than the declared return type.
            //
            // assert(
            //    returnedReferenceValue.isNull.isYes ||
            //         classHierarchy.isASubtypeOf(
            //             returnedReferenceValue.upperTypeBound,
            //            method.returnType.asObjectType
            //        ).isYesOrUnknown,
            //     s"$returnedReferenceValue is not a subtype of the return type ${method.returnType}"
            // )
            if (isUpdated) {
                val returnedValueUTB = returnedReferenceValue.upperTypeBound
                val methodReturnType = method.returnType.asObjectType
                if (!classHierarchy.isSubtypeOf(returnedValueUTB, methodReturnType)) {
                    // the type hierarchy is incomplete...
                    ai.interrupt()
                } else if (returnedReferenceValue.isNull.isUnknown &&
                    returnedValueUTB.isSingletonSet &&
                    returnedValueUTB.head == methodReturnType &&
                    !returnedReferenceValue.isPrecise) {
                    // we don't get more precise information
                    ai.interrupt()
                }
            }
            isUpdated // <= whether the information about the returned value was updated
        }
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
        dependees.updateAll() // <= doesn't hurt if dependees is empty

        val domain = new MethodReturnValuesAnalysisDomain(ai, method, dependees)
        val aiResult = ai(method, domain) // the state is implicitly accumulated in the domain

        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            ai.resetInterrupt()
            analyze(
                ai,
                method,
                // We need a new instance, because the set may grow when compared to the
                // last run; actually it may even shrink, but this is will likely only happen in
                // very rare cases and is not a problem.
                dependees.clone()
            )
        }

        if (!aiResult.wasAborted) {
            val vi: Option[ValueInformation] = aiResult.domain.returnedValue.map(_.toCanonicalForm)
            if (dependees.isEmpty
                || vi.isEmpty // <=> the method always ends with an exception or not at all
                // THE FOLLOWING TESTS WOULD REQUIRE ADDITIONAL KNOWLEDGE ABOUT THE DEPENDEES!
                // IN GENERAL, EVERY POSSIBLE REFINEMENT COULD LEAD TO THE CASE THAT THE CURRENT
                // METHOD WILL ALWAYS THROW AN EXCEPTION!
                // || vi.get.asReferenceValue.isNull.isYes
                // || (vi.get.asReferenceValue.isPrecise && vi.get.asReferenceValue.isNull.isNo)
                ) {
                Result(method, MethodReturnValue(vi))
            } else {
                // We have potentially relevant dependencies (please, recall that we are currently
                // not flow-sensitive).
                InterimResult.forLB(method, MethodReturnValue(vi), dependees.toSet, c)
            }
        } else {
            //... in this run (!) no refinement was possible and therefore we had an early
            // return (interrupt), but if we have dependencies, further refinements are still
            // possible, e.g., because some path(s) may be pruned by future refinements or
            // more precise type information becomes available.
            val mrv = TheMethodReturnValue(ValueInformation.forProperValue(method.returnType))
            if (dependees.isEmpty) {
                Result(FinalEP(method, mrv))
            } else {
                InterimResult.forLB(method, mrv, dependees.toSet, c)
            }
        }
    }

}

object EagerLBMethodReturnValuesAnalysis extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq.empty

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        // To ensure that subsequent analyses are able to pick-up the results of this
        // analysis, we state that the domain that has to be used when computing
        // the AIResult has to use the (partial) domain: RefinedTypeLevelInvokeInstructions.
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey)(
            i => i.getOrElse(Set.empty) + classOf[RefinedTypeLevelInvokeInstructions]
        )
        null
    }

    override def uses: Set[PropertyBounds] = Set.empty

    def derivedProperty: PropertyBounds = PropertyBounds.lb(MethodReturnValue.key)

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new LBMethodReturnValuesAnalysis(p)
        val methods = p.allMethodsWithBody.iterator.filter { m =>
            val returnType = m.returnType
            returnType.isObjectType
            // If we enable the following check then we can't refine to null anymore:
            // && p.classHierarchy.hasSubtypes(returnType.asObjectType).isYes
        }
        ps.scheduleEagerComputationsForEntities(methods)(analysis.analyze)
        analysis
    }
}
