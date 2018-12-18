/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.fpcf.properties.VirtualMethodAllocationFreeness
import org.opalj.fpcf.properties.AllocationFreeness
import org.opalj.fpcf.properties.MethodWithAllocations
import org.opalj.fpcf.properties.AllocationFreeMethod
import org.opalj.fpcf.properties.VirtualMethodAllocationFreeness.VMethodWithAllocations
import org.opalj.fpcf.properties.VirtualMethodAllocationFreeness.VAllocationFreeMethod

/**
 * Determines the aggregated allocation freeness for virtual methods.
 *
 * @author Dominik Helm
 */
class VirtualMethodAllocationFreenessAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    def determineAllocationFreeness(dm: DeclaredMethod): ProperPropertyComputationResult = {
        if (!dm.hasSingleDefinedMethod && !dm.hasMultipleDefinedMethods)
            return Result(dm, VMethodWithAllocations);

        var dependees: Set[EOptionP[DeclaredMethod, AllocationFreeness]] = Set.empty

        val cfo = if (dm.declaringClassType.isArrayType) project.ObjectClassFile
        else project.classFile(dm.declaringClassType.asObjectType)
        val methods =
            if (cfo.isDefined && cfo.get.isInterfaceDeclaration)
                project.interfaceCall(dm.declaringClassType.asObjectType, dm.name, dm.descriptor)
            else if (dm.hasSingleDefinedMethod && dm.definedMethod.isPackagePrivate)
                project.virtualCall(
                    dm.definedMethod.classFile.thisType.packageName,
                    dm.declaringClassType,
                    dm.name,
                    dm.descriptor
                )
            else project.virtualCall(
                "" /* package is irrelevant, must be public interface methods */ ,
                dm.declaringClassType,
                dm.name,
                dm.descriptor
            )

        for (method ← methods) {
            propertyStore(declaredMethods(method), AllocationFreeness.key) match {
                case FinalP(AllocationFreeMethod)  ⇒
                case FinalP(MethodWithAllocations) ⇒ return Result(dm, VMethodWithAllocations);
                case epk                           ⇒ dependees += epk
            }
        }

        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            dependees = dependees.filter { _.e ne eps.e }

            eps match {
                case FinalP(AllocationFreeMethod)  ⇒
                case FinalP(MethodWithAllocations) ⇒ return Result(dm, VMethodWithAllocations);
                case epk ⇒
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
            case m: DeclaredMethod ⇒ determineAllocationFreeness(m)
            case _                 ⇒ throw new IllegalArgumentException(s"$e ist not a method")
        }
    }

}

trait VirtualMethodAllocationFreenessAnalysisScheduler extends ComputationSpecification[FPCFAnalysis] {

    final def derivedProperty: PropertyBounds = {
        PropertyBounds.lub(VirtualMethodAllocationFreeness)
    }

    final override def uses: Set[PropertyBounds] = Set(PropertyBounds.lub(AllocationFreeness))

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
