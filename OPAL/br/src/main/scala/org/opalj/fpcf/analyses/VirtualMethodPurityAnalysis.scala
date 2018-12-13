/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.fpcf.properties.ClassifiedImpure
import org.opalj.fpcf.properties.CompileTimePure
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.VirtualMethodPurity.VImpureByAnalysis
import org.opalj.fpcf.properties.VirtualMethodPurity.VImpureByLackOfInformation
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject

/**
 * Determines the aggregated purity for virtual methods.
 *
 * @author Dominik Helm
 */
class VirtualMethodPurityAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    def determinePurity(dm: DefinedMethod): ProperPropertyComputationResult = {
        if (!dm.hasSingleDefinedMethod && !dm.hasMultipleDefinedMethods)
            return Result(dm, VImpureByLackOfInformation);

        var maxPurity: Purity = CompileTimePure
        var dependees: Set[EOptionP[DeclaredMethod, Purity]] = Set.empty

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
            propertyStore(declaredMethods(method), Purity.key) match {
                case eps @ UBP(ub) ⇒
                    maxPurity = maxPurity meet ub
                    if (eps.isRefinable) dependees += eps
                case epk ⇒ dependees += epk
            }
        }

        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            dependees = dependees.filter { _.e ne eps.e }
            maxPurity = maxPurity meet eps.ub.asInstanceOf[Purity]
            if (eps.isRefinable) {
                dependees += eps.asInstanceOf[EOptionP[DeclaredMethod, Purity]]
            }

            if (dependees.isEmpty || maxPurity.isInstanceOf[ClassifiedImpure]) {
                Result(dm, maxPurity.aggregatedProperty)
            } else {
                InterimResult(
                    dm, VImpureByAnalysis, maxPurity.aggregatedProperty,
                    dependees, c
                )
            }
        }

        if (dependees.isEmpty || maxPurity.isInstanceOf[ClassifiedImpure]) {
            Result(dm, maxPurity.aggregatedProperty)
        } else {
            InterimResult(
                dm, VImpureByAnalysis, maxPurity.aggregatedProperty,
                dependees, c
            )
        }
    }

    /** Called when the analysis is scheduled lazily. */
    def doDeterminePurity(e: Entity): ProperPropertyComputationResult = {
        e match {
            case m: DefinedMethod ⇒ determinePurity(m)
            case _                ⇒ throw new IllegalArgumentException(s"$e ist not a DefinedMethod")
        }
    }

}

trait VirtualMethodPurityAnalysisScheduler extends ComputationSpecification {

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(VirtualMethodPurity)

    final override def uses: Set[PropertyBounds] = Set(PropertyBounds.lub(Purity))

}

object EagerVirtualMethodPurityAnalysis
    extends VirtualMethodPurityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodPurityAnalysis(p)
        val vms = p.get(DeclaredMethodsKey)
        val configuredPurity = p.get(ConfiguredPurityKey)
        ps.scheduleEagerComputationsForEntities(
            vms.declaredMethods.filter { dm ⇒
                !configuredPurity.wasSet(dm) && dm.isInstanceOf[DefinedMethod]
            }.map(_.asInstanceOf[DefinedMethod])
        )(analysis.determinePurity)
        analysis
    }
}

object LazyVirtualMethodPurityAnalysis
    extends VirtualMethodPurityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodPurityAnalysis(p)
        ps.registerLazyPropertyComputation(VirtualMethodPurity.key, analysis.doDeterminePurity)
        analysis
    }
}
