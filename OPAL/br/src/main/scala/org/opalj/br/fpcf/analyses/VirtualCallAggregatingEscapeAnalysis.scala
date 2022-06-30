/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

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
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.GlobalEscape
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.br.fpcf.properties.VirtualMethodEscapeProperty

/**
 * Aggregates the escape information for virtual formal parameters.
 * That are all possible call targets that override the method attached to the virtual method.
 *
 * @see
 *      [[org.opalj.br.fpcf.properties.EscapeProperty]]
 *      [[org.opalj.br.analyses.VirtualFormalParameter]]
 *
 * @author Florian Kuebler
 */
class VirtualCallAggregatingEscapeAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val formalParameters = project.get(VirtualFormalParametersKey)
    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    def determineEscape(fp: VirtualFormalParameter): ProperPropertyComputationResult = {
        val dm = fp.method
        assert(!dm.isInstanceOf[VirtualDeclaredMethod])

        if (dm.declaringClassType.isArrayType) {
            ??? //TODO handle case
        }

        // ANALYSIS STATE
        var escapeState: EscapeProperty = NoEscape
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
            val vfp = formalParameters(declaredMethods(method))(-1 - fp.origin)
            handleEscapeState(propertyStore(vfp, EscapeProperty.key))
        }

        def handleEscapeState(eOptionP: EOptionP[VirtualFormalParameter, EscapeProperty]): Unit =
            eOptionP match {
                case ep @ InterimUBP(p) =>
                    escapeState = escapeState meet p
                    dependees += ep
                case FinalP(p) => escapeState = escapeState meet p
                case epk       => dependees += epk
            }

        def returnResult: ProperPropertyComputationResult = {
            if (escapeState.isBottom || dependees.isEmpty)
                if (escapeState.isInstanceOf[AtMost])
                    //InterimResult(fp, GlobalEscape.asAggregatedProperty, escapeState.asAggregatedProperty, dependees, continuation)
                    Result(fp, escapeState.asAggregatedProperty)
                else
                    Result(fp, escapeState.asAggregatedProperty)
            else
                InterimResult(
                    fp, GlobalEscape.asAggregatedProperty, escapeState.asAggregatedProperty,
                    dependees, c
                )
        }

        def c(someEPS: SomeEPS): ProperPropertyComputationResult = {
            val other = someEPS.e

            assert(dependees.count(_.e eq other) <= 1)
            dependees = dependees filter { _.e ne other }
            handleEscapeState(someEPS.asInstanceOf[EOptionP[VirtualFormalParameter, EscapeProperty]])

            returnResult
        }

        returnResult
    }

}

sealed trait VirtualCallAggregatingEscapeAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        VirtualFormalParametersKey,
        DeclaredMethodsKey
    )

    final override def uses: Set[PropertyBounds] = Set(PropertyBounds.lub(EscapeProperty))

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(VirtualMethodEscapeProperty)

}

object EagerVirtualCallAggregatingEscapeAnalysis
    extends VirtualCallAggregatingEscapeAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualCallAggregatingEscapeAnalysis(p)
        val vfps = p.get(VirtualFormalParametersKey).virtualFormalParameters
        ps.scheduleEagerComputationsForEntities(vfps)(analysis.determineEscape)
        analysis
    }
}

object LazyVirtualCallAggregatingEscapeAnalysis
    extends VirtualCallAggregatingEscapeAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualCallAggregatingEscapeAnalysis(p)
        ps.registerLazyPropertyComputation(VirtualMethodEscapeProperty.key, analysis.determineEscape)
        analysis
    }

}
