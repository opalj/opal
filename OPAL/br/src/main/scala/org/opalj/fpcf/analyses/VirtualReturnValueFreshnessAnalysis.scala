/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VNoFreshReturnValue
import org.opalj.fpcf.properties.VPrimitiveReturnValue
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness

/**
 * An analysis that aggregates whether the return value for all possible methods represented by a
 * given [[org.opalj.br.DeclaredMethod]] are always freshly allocated.
 *
 * @author Florian Kuebler
 */
class VirtualReturnValueFreshnessAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {
    private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def determineFreshness(m: DeclaredMethod): PropertyComputationResult = {
        if (m.descriptor.returnType.isBaseType || m.descriptor.returnType.isVoidType) {
            return Result(m, VPrimitiveReturnValue)
        }

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty

        if (m.declaringClassType.isArrayType) {
            throw new NotImplementedError()
        }

        val methods = project.virtualCall(
            m.declaringClassType.asObjectType.packageName,
            m.declaringClassType,
            m.name,
            m.descriptor
        )

        var temporary: VirtualMethodReturnValueFreshness = VFreshReturnValue

        for (method ← methods) {
            val rvf = propertyStore(declaredMethods(method), ReturnValueFreshness.key)
            handleReturnValueFreshness(rvf).foreach(return _)
        }

        def handleReturnValueFreshness(
            eOptionP: EOptionP[DeclaredMethod, ReturnValueFreshness]
        ): Option[PropertyComputationResult] = eOptionP match {
            case FinalP(_, NoFreshReturnValue) ⇒
                Some(Result(m, VNoFreshReturnValue))

            case FinalP(_, PrimitiveReturnValue) ⇒
                throw new RuntimeException("unexpected property")

            case ep @ EPS(_, _, p) ⇒
                temporary = temporary meet p.asVirtualMethodReturnValueFreshness
                if (ep.isRefinable)
                    dependees += ep
                None

            case epk ⇒
                dependees += epk
                None
        }

        def returnResult(): PropertyComputationResult = {
            if (dependees.isEmpty)
                Result(m, temporary)
            else
                InterimResult(m, VNoFreshReturnValue, temporary, dependees, c)
        }

        def c(someEPS: SomeEPS): PropertyComputationResult = {

            dependees = dependees.filter(_.e ne someEPS.e)

            handleReturnValueFreshness(
                someEPS.asInstanceOf[EOptionP[DeclaredMethod, ReturnValueFreshness]]
            ).foreach(return _)

            returnResult()
        }

        returnResult()
    }

}

sealed trait VirtualReturnValueFreshnessAnalysisScheduler extends ComputationSpecification {

    final override def derives: Set[PropertyKind] = Set(VirtualMethodReturnValueFreshness)

    final override def uses: Set[PropertyKind] = Set(ReturnValueFreshness)

    final type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

object EagerVirtualReturnValueFreshnessAnalysis
    extends VirtualReturnValueFreshnessAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val declaredMethods = p.get(DeclaredMethodsKey).declaredMethods
        val analysis = new VirtualReturnValueFreshnessAnalysis(p)
        ps.scheduleEagerComputationsForEntities(declaredMethods)(
            analysis.determineFreshness
        )
        analysis
    }
}

object LazyVirtualReturnValueFreshnessAnalysis
    extends VirtualReturnValueFreshnessAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    override def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualReturnValueFreshnessAnalysis(p)
        ps.registerLazyPropertyComputation(
            VirtualMethodReturnValueFreshness.key,
            analysis.determineFreshness
        )
        analysis
    }
}
