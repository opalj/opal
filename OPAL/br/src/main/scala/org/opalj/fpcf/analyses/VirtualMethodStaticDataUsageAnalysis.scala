/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.fpcf.properties.VirtualMethodAllocationFreeness
import org.opalj.fpcf.properties.VirtualMethodStaticDataUsage
import org.opalj.fpcf.properties.StaticDataUsage
import org.opalj.fpcf.properties.UsesNoStaticData
import org.opalj.fpcf.properties.UsesVaryingData
import org.opalj.fpcf.properties.UsesConstantDataOnly
import org.opalj.fpcf.properties.VirtualMethodStaticDataUsage.VUsesVaryingData

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

        var dependees: Set[EOptionP[DeclaredMethod, StaticDataUsage]] = Set.empty

        var maxLevel: StaticDataUsage = UsesNoStaticData

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
            propertyStore(declaredMethods(method), StaticDataUsage.key) match {
                case FinalP(UsesNoStaticData)     ⇒
                case FinalP(UsesConstantDataOnly) ⇒ maxLevel = UsesConstantDataOnly
                case FinalP(UsesVaryingData)      ⇒ return Result(dm, VUsesVaryingData);
                case ep @ InterimUBP(UsesConstantDataOnly) ⇒
                    maxLevel = UsesConstantDataOnly
                    dependees += ep
                case epk ⇒ dependees += epk
            }
        }

        def c(eps: SomeEPS): ProperPropertyComputationResult = {
            dependees = dependees.filter { _.e ne eps.e }

            eps match {
                case FinalP(UsesNoStaticData)     ⇒
                case FinalP(UsesConstantDataOnly) ⇒ maxLevel = UsesConstantDataOnly
                case FinalP(UsesVaryingData)      ⇒ return Result(dm, VUsesVaryingData);
                case ep @ InterimUBP(UsesConstantDataOnly) ⇒
                    maxLevel = UsesConstantDataOnly
                    dependees += ep.asInstanceOf[EOptionP[DeclaredMethod, StaticDataUsage]]
                case epk ⇒ dependees += epk.asInstanceOf[EOptionP[DeclaredMethod, StaticDataUsage]]
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
            InterimResult(
                dm, VUsesVaryingData, maxLevel.aggregatedProperty,
                dependees, c
            )
        }
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineUsage(e: Entity): ProperPropertyComputationResult = {
        e match {
            case m: DeclaredMethod ⇒ determineUsage(m)
            case _                 ⇒ throw new IllegalArgumentException(s"$e is not a method")
        }
    }

}

trait VirtualMethodStaticDataUsageAnalysisScheduler extends ComputationSpecification {

    final override def derives: Set[PropertyKind] = Set(VirtualMethodStaticDataUsage)

    final override def uses: Set[PropertyKind] = Set(StaticDataUsage)

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

object EagerVirtualMethodStaticDataUsageAnalysis
    extends VirtualMethodStaticDataUsageAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodStaticDataUsageAnalysis(p)
        val vms = p.get(DeclaredMethodsKey)
        ps.scheduleEagerComputationsForEntities(vms.declaredMethods)(analysis.determineUsage)
        analysis
    }
}

object LazyVirtualMethodStaticDataUsageAnalysis
    extends VirtualMethodStaticDataUsageAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodStaticDataUsageAnalysis(p)
        ps.registerLazyPropertyComputation(
            VirtualMethodAllocationFreeness.key,
            analysis.doDetermineUsage
        )
        analysis
    }
}
