/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.NoFreshReturnValue
import org.opalj.br.fpcf.properties.PrimitiveReturnValue
import org.opalj.br.fpcf.properties.ReturnValueFreshness
import org.opalj.br.fpcf.properties.VFreshReturnValue
import org.opalj.br.fpcf.properties.VirtualMethodReturnValueFreshness
import org.opalj.br.fpcf.properties.VNoFreshReturnValue
import org.opalj.br.fpcf.properties.VPrimitiveReturnValue

/**
 * An analysis that aggregates whether the return value for all possible methods represented by a
 * given [[org.opalj.br.DeclaredMethod]] are always freshly allocated.
 *
 * @author Florian Kübler
 */
class VirtualReturnValueFreshnessAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def determineFreshness(dm: DeclaredMethod): ProperPropertyComputationResult = {
        if (dm.descriptor.returnType.isBaseType || dm.descriptor.returnType.isVoidType) {
            return Result(dm, VPrimitiveReturnValue)
        }

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty

        val cfo = project.classFile(dm.declaringClassType)
        val methods =
            if (cfo.isDefined && cfo.get.isInterfaceDeclaration)
                project.interfaceCall(
                    dm.declaringClassType,
                    dm.declaringClassType,
                    dm.name,
                    dm.descriptor
                )
            else project.virtualCall(
                dm.declaringClassType,
                dm.declaringClassType,
                dm.name,
                dm.descriptor
            )

        var temporary: VirtualMethodReturnValueFreshness = VFreshReturnValue

        for (method <- methods) {
            val rvf = propertyStore(declaredMethods(method), ReturnValueFreshness.key)
            handleReturnValueFreshness(rvf).foreach(return _)
        }

        def handleReturnValueFreshness(
            eOptionP: EOptionP[DeclaredMethod, ReturnValueFreshness]
        ): Option[ProperPropertyComputationResult] = eOptionP match {
            case FinalP(NoFreshReturnValue) =>
                Some(Result(dm, VNoFreshReturnValue))

            case FinalP(PrimitiveReturnValue) =>
                throw new RuntimeException("unexpected property")

            case ep @ UBP(p) =>
                temporary = temporary meet p.asVirtualMethodReturnValueFreshness
                if (ep.isRefinable)
                    dependees += ep
                None

            case epk =>
                dependees += epk
                None
        }

        def returnResult(): ProperPropertyComputationResult = {
            if (dependees.isEmpty)
                Result(dm, temporary)
            else
                InterimResult(dm, VNoFreshReturnValue, temporary, dependees, c)
        }

        def c(someEPS: SomeEPS): ProperPropertyComputationResult = {

            dependees = dependees.filter(_.e ne someEPS.e)

            val r = handleReturnValueFreshness(
                someEPS.asInstanceOf[EOptionP[DeclaredMethod, ReturnValueFreshness]]
            )
            if (r.isDefined) return r.get;

            returnResult()
        }

        returnResult()
    }

}

sealed trait VirtualReturnValueFreshnessAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    final def derivedProperty: PropertyBounds = {
        PropertyBounds.lub(VirtualMethodReturnValueFreshness)
    }

    final override def uses: Set[PropertyBounds] = Set(PropertyBounds.lub(ReturnValueFreshness))

}

object EagerVirtualReturnValueFreshnessAnalysis
    extends VirtualReturnValueFreshnessAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

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
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualReturnValueFreshnessAnalysis(p)
        ps.registerLazyPropertyComputation(
            VirtualMethodReturnValueFreshness.key,
            analysis.determineFreshness
        )
        analysis
    }
}
