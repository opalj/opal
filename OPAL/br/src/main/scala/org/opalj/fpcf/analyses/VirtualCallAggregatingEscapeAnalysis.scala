/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analyses

import org.opalj.br.DefinedMethod
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
        assert(dm.isInstanceOf[DefinedMethod])
        val m = dm.methodDefinition

        if (dm.declaringClassType.isArrayType) {
            ??? //TODO handle case
        }

        // ANALYSIS STATE
        var escapeState: EscapeProperty = NoEscape
        var dependees: Set[EOptionP[VirtualFormalParameter, EscapeProperty]] = Set.empty

        val maybeFile = project.classFile(dm.declaringClassType.asObjectType)

        val methods = if (maybeFile.isDefined && maybeFile.get.isInterfaceDeclaration) {
            project.interfaceCall(
                /* use the package in which the concrete method context is defined */
                dm.declaringClassType.asObjectType, dm.name, dm.descriptor
            )
        } else {
            project.virtualCall(
                /* use the package in which the concrete method context is defined */
                m.classFile.thisType.packageName,
                dm.declaringClassType, dm.name, dm.descriptor
            )
        }

        for (method ← methods) {
            val vfp = formalParameters(declaredMethods(method))(-1 - fp.origin)
            handleEscapeState(propertyStore(vfp, EscapeProperty.key))
        }

        def handleEscapeState(eOptionP: EOptionP[VirtualFormalParameter, EscapeProperty]): Unit =
            eOptionP match {
                case ep @ IntermediateEP(_, _, p) ⇒
                    escapeState = escapeState meet p
                    dependees += ep
                case FinalEP(_, p) ⇒ escapeState = escapeState meet p
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
                IntermediateResult(fp, GlobalEscape.asAggregatedProperty, escapeState.asAggregatedProperty, dependees, continuation)
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

    override def derives: Set[PropertyKind] = Set(VirtualMethodEscapeProperty)

    override def uses: Set[PropertyKind] = Set(EscapeProperty)
}

object EagerVirtualCallAggregatingEscapeAnalysis
        extends VirtualCallAggregatingEscapeAnalysisScheduler
        with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualCallAggregatingEscapeAnalysis(project)
        val vfps = project.get(VirtualFormalParametersKey).virtualFormalParameters
        propertyStore.scheduleEagerComputationsForEntities(vfps)(analysis.determineEscape)
        analysis
    }
}
object LazyVirtualCallAggregatingEscapeAnalysis
        extends VirtualCallAggregatingEscapeAnalysisScheduler
        with FPCFLazyAnalysisScheduler {
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualCallAggregatingEscapeAnalysis(project)
        propertyStore.registerLazyPropertyComputation(VirtualMethodEscapeProperty.key, analysis.determineEscape)
        analysis
    }
}
