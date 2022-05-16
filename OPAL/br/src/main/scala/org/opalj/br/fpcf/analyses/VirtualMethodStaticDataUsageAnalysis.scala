/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.SomeEPS
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.StaticDataUsage
import org.opalj.br.fpcf.properties.UsesConstantDataOnly
import org.opalj.br.fpcf.properties.UsesNoStaticData
import org.opalj.br.fpcf.properties.UsesVaryingData
import org.opalj.br.fpcf.properties.VirtualMethodAllocationFreeness
import org.opalj.br.fpcf.properties.VirtualMethodStaticDataUsage
import org.opalj.br.fpcf.properties.VirtualMethodStaticDataUsage.VUsesVaryingData

/**
 * Determines the aggregated static data usage for virtual methods.
 *
 * @author Dominik Helm
 */
class VirtualMethodStaticDataUsageAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {
    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    def determineUsage(dm: DeclaredMethod): ProperPropertyComputationResult = {
        if (!dm.hasSingleDefinedMethod && !dm.hasMultipleDefinedMethods)
            return Result(dm, VUsesVaryingData);

        var dependees: Set[SomeEOptionP] = Set.empty

        var maxLevel: StaticDataUsage = UsesNoStaticData

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
            propertyStore(declaredMethods(method), StaticDataUsage.key) match {
                case FinalP(UsesNoStaticData)     =>
                case FinalP(UsesConstantDataOnly) => maxLevel = UsesConstantDataOnly
                case FinalP(UsesVaryingData)      => return Result(dm, VUsesVaryingData);
                case ep @ InterimUBP(UsesConstantDataOnly) =>
                    maxLevel = UsesConstantDataOnly
                    dependees += ep
                case epk => dependees += epk
            }
        }

        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            dependees = dependees.filter { _.e ne eps.e }

            eps match {
                case FinalP(UsesNoStaticData)     =>
                case FinalP(UsesConstantDataOnly) => maxLevel = UsesConstantDataOnly
                case FinalP(UsesVaryingData)      => return Result(dm, VUsesVaryingData);
                case ep @ InterimUBP(UsesConstantDataOnly) =>
                    maxLevel = UsesConstantDataOnly
                    dependees += ep.asInstanceOf[EOptionP[DeclaredMethod, StaticDataUsage]]
                case epk => dependees += epk.asInstanceOf[EOptionP[DeclaredMethod, StaticDataUsage]]
            }

            if (dependees.isEmpty) {
                Result(dm, maxLevel.aggregatedProperty)
            } else {
                InterimResult(
                    dm, VUsesVaryingData, maxLevel.aggregatedProperty,
                    dependees, c
                )
            }
        }

        if (dependees.isEmpty) {
            Result(dm, maxLevel.aggregatedProperty)
        } else {
            org.opalj.fpcf.InterimResult(
                dm, VUsesVaryingData, maxLevel.aggregatedProperty,
                dependees, c
            )
        }
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineUsage(e: Entity): ProperPropertyComputationResult = {
        e match {
            case m: DeclaredMethod => determineUsage(m)
            case _                 => throw new IllegalArgumentException(s"$e is not a method")
        }
    }

}

trait VirtualMethodStaticDataUsageAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    final override def uses: Set[PropertyBounds] = Set(PropertyBounds.lub(StaticDataUsage))

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(VirtualMethodStaticDataUsage)

}

object EagerVirtualMethodStaticDataUsageAnalysis
    extends VirtualMethodStaticDataUsageAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodStaticDataUsageAnalysis(p)
        val vms = p.get(DeclaredMethodsKey)
        ps.scheduleEagerComputationsForEntities(vms.declaredMethods)(analysis.determineUsage)
        analysis
    }
}

object LazyVirtualMethodStaticDataUsageAnalysis
    extends VirtualMethodStaticDataUsageAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodStaticDataUsageAnalysis(p)
        ps.registerLazyPropertyComputation(
            VirtualMethodAllocationFreeness.key,
            analysis.doDetermineUsage
        )
        analysis
    }
}
