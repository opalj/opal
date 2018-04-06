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
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.fpcf.properties.Conditional
import org.opalj.fpcf.properties.EscapeProperty
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
class VirtualCallAggregatingEscapeAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val formalParameters = propertyStore.context[VirtualFormalParameters]
    private[this] val declaredMethods = propertyStore.context[DeclaredMethods]

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
                case ep @ EP(_, Conditional(p)) ⇒
                    escapeState = escapeState meet p
                    dependees += ep
                case EP(_, p) ⇒ escapeState = escapeState meet p
                case epk      ⇒ dependees += epk
            }

        def returnResult: PropertyComputationResult = {
            if (escapeState.isBottom || dependees.isEmpty)
                if (escapeState.isRefinable)
                    RefinableResult(fp, VirtualMethodEscapeProperty(escapeState))
                else
                    Result(fp, VirtualMethodEscapeProperty(escapeState))
            else
                IntermediateResult(fp, VirtualMethodEscapeProperty(Conditional(escapeState)), dependees, continuation)
        }

        def continuation(other: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
            if (p eq PropertyIsLazilyComputed)
                IntermediateResult(
                    fp, VirtualMethodEscapeProperty(Conditional(escapeState)), dependees, continuation
                )

            val newEP = EP(
                other.asInstanceOf[VirtualFormalParameter], p.asInstanceOf[EscapeProperty]
            )

            assert(dependees.count(_.e eq other) <= 1)
            dependees = dependees filter { _.e ne other }
            handleEscapeState(newEP)

            returnResult
        }

        returnResult
    }

}

object VirtualCallAggregatingEscapeAnalysis extends FPCFAnalysisScheduler {

    override def derivedProperties: Set[PropertyKind] = Set(VirtualMethodEscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set(EscapeProperty)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualCallAggregatingEscapeAnalysis(project)
        val vfps = propertyStore.context[VirtualFormalParameters].virtualFormalParameters
        propertyStore.scheduleForEntities(vfps)(analysis.determineEscape)
        analysis
    }

    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualCallAggregatingEscapeAnalysis(project)
        propertyStore.scheduleLazyPropertyComputation(VirtualMethodEscapeProperty.key, analysis.determineEscape)
        analysis
    }
}