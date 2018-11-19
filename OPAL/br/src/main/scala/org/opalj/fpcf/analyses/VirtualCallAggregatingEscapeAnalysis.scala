/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.VirtualDeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.GlobalEscape
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.VirtualMethodEscapeProperty

/**
 * Aggregates the escape information for virtual formal parameters.
 * That are all possible call targets that override the method attached to the virtual method.
 *
 * @see
 *      [[org.opalj.fpcf.properties.EscapeProperty]]
 *      [[org.opalj.br.analyses.VirtualFormalParameter]]
 *
 * @author Florian Kuebler
 */
class VirtualCallAggregatingEscapeAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val formalParameters = project.get(VirtualFormalParametersKey)
    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    def determineEscape(fp: VirtualFormalParameter): PropertyComputationResult = {
        val dm = fp.method
        assert(!dm.isInstanceOf[VirtualDeclaredMethod])

        if (dm.declaringClassType.isArrayType) {
            ??? //TODO handle case
        }

        // ANALYSIS STATE
        var escapeState: EscapeProperty = NoEscape
        var dependees: Set[EOptionP[VirtualFormalParameter, EscapeProperty]] = Set.empty

        val maybeFile = project.classFile(dm.declaringClassType.asObjectType)

        val methods =
            if (maybeFile.isDefined && maybeFile.get.isInterfaceDeclaration)
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
            val vfp = formalParameters(declaredMethods(method))(-1 - fp.origin)
            handleEscapeState(propertyStore(vfp, EscapeProperty.key))
        }

        def handleEscapeState(eOptionP: EOptionP[VirtualFormalParameter, EscapeProperty]): Unit =
            eOptionP match {
                case ep @ IntermediateEP(_, _, p) ⇒
                    escapeState = escapeState meet p
                    dependees += ep
                case FinalP(_, p) ⇒ escapeState = escapeState meet p
                case epk           ⇒ dependees += epk
            }

        def returnResult: PropertyComputationResult = {
            if (escapeState.isBottom || dependees.isEmpty)
                if (escapeState.isInstanceOf[AtMost])
                    //IntermediateResult(fp, GlobalEscape.asAggregatedProperty, escapeState.asAggregatedProperty, dependees, continuation)
                    Result(fp, escapeState.asAggregatedProperty)
                else
                    Result(fp, escapeState.asAggregatedProperty)
            else
                IntermediateResult(
                    fp, GlobalEscape.asAggregatedProperty, escapeState.asAggregatedProperty,
                    dependees, continuation
                )
        }

        def continuation(someEPS: SomeEPS): PropertyComputationResult = {
            val other = someEPS.e

            assert(dependees.count(_.e eq other) <= 1)
            dependees = dependees filter { _.e ne other }
            handleEscapeState(someEPS.asInstanceOf[EOptionP[VirtualFormalParameter, EscapeProperty]])

            returnResult
        }

        returnResult
    }

}

sealed trait VirtualCallAggregatingEscapeAnalysisScheduler extends ComputationSpecification {

    final override def derives: Set[PropertyKind] = Set(VirtualMethodEscapeProperty)

    final override def uses: Set[PropertyKind] = Set(EscapeProperty)

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

object EagerVirtualCallAggregatingEscapeAnalysis
    extends VirtualCallAggregatingEscapeAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualCallAggregatingEscapeAnalysis(p)
        val vfps = p.get(VirtualFormalParametersKey).virtualFormalParameters
        ps.scheduleEagerComputationsForEntities(vfps)(analysis.determineEscape)
        analysis
    }
}

object LazyVirtualCallAggregatingEscapeAnalysis
    extends VirtualCallAggregatingEscapeAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    override def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualCallAggregatingEscapeAnalysis(p)
        ps.registerLazyPropertyComputation(VirtualMethodEscapeProperty.key, analysis.determineEscape)
        analysis
    }

}
