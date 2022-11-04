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
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.AllocationFreeMethod
import org.opalj.br.fpcf.properties.AllocationFreeness
import org.opalj.br.fpcf.properties.MethodWithAllocations
import org.opalj.br.fpcf.properties.VirtualMethodAllocationFreeness
import org.opalj.br.fpcf.properties.VirtualMethodAllocationFreeness.VAllocationFreeMethod
import org.opalj.br.fpcf.properties.VirtualMethodAllocationFreeness.VMethodWithAllocations

/**
 * Determines the aggregated allocation freeness for virtual methods.
 *
 * @author Dominik Helm
 */
class VirtualMethodAllocationFreenessAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    def determineAllocationFreeness(dm: DeclaredMethod): ProperPropertyComputationResult = {
        if (!dm.hasSingleDefinedMethod && !dm.hasMultipleDefinedMethods)
            return Result(dm, VMethodWithAllocations);

        var dependees: Set[SomeEOptionP] = Set.empty

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

        for (method <- methods) {
            propertyStore(declaredMethods(method), AllocationFreeness.key) match {
                case FinalP(AllocationFreeMethod)  =>
                case FinalP(MethodWithAllocations) => return Result(dm, VMethodWithAllocations);
                case epk                           => dependees += epk
            }
        }

        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            dependees = dependees.filter { _.e ne eps.e }

            eps match {
                case FinalP(AllocationFreeMethod)  =>
                case FinalP(MethodWithAllocations) => return Result(dm, VMethodWithAllocations);
                case epk =>
                    dependees += epk.asInstanceOf[EOptionP[DeclaredMethod, AllocationFreeness]]
            }

            if (dependees.isEmpty) {
                Result(dm, VAllocationFreeMethod)
            } else {
                InterimResult(dm, VMethodWithAllocations, VAllocationFreeMethod, dependees, c)
            }
        }

        if (dependees.isEmpty) {
            Result(dm, VAllocationFreeMethod)
        } else {
            InterimResult(dm, VMethodWithAllocations, VAllocationFreeMethod, dependees, c)
        }
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineAllocationFreeness(e: Entity): ProperPropertyComputationResult = {
        e match {
            case m: DeclaredMethod => determineAllocationFreeness(m)
            case _                 => throw new IllegalArgumentException(s"$e ist not a method")
        }
    }

}

trait VirtualMethodAllocationFreenessAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    final override def uses: Set[PropertyBounds] = Set(PropertyBounds.lub(AllocationFreeness))

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(VirtualMethodAllocationFreeness)

}

object EagerVirtualMethodAllocationFreenessAnalysis
    extends VirtualMethodAllocationFreenessAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodAllocationFreenessAnalysis(p)
        val dms = p.get(DeclaredMethodsKey).declaredMethods
        ps.scheduleEagerComputationsForEntities(dms)(analysis.determineAllocationFreeness)
        analysis
    }
}

object LazyVirtualMethodAllocationFreenessAnalysis
    extends VirtualMethodAllocationFreenessAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodAllocationFreenessAnalysis(p)
        ps.registerLazyPropertyComputation(
            VirtualMethodAllocationFreeness.key,
            analysis.doDetermineAllocationFreeness
        )
        analysis
    }
}
