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

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.fpcf.properties.AtMost
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
 * @author Florian Kübler
 */
class VirtualCallAggregatingEscapeAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val formalParameters = propertyStore.context[VirtualFormalParameters]
    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    def determineEscape(fp: VirtualFormalParameter): PropertyComputationResult = {
        val dm = fp.method
        if (dm.declaringClassType.isArrayType) {
            throw new NotImplementedError()
            //TODO handle case
        }
        var escapeState: EscapeProperty = NoEscape
        var dependees: Set[EOptionP[VirtualFormalParameter, EscapeProperty]] = Set.empty
        val methods = project.virtualCall(
            /* use the package in which the concrete method context is defined */
            dm.declaringClassType.asObjectType.packageName,
            dm.declaringClassType, dm.name, dm.descriptor
        )
        for (method ← methods) {
            propertyStore(formalParameters(declaredMethods(method))(-1 - fp.origin), EscapeProperty.key) match {
                case EP(_, p) if p.isFinal ⇒ escapeState = escapeState meet p
                case EP(_, AtMost(p))      ⇒ escapeState = escapeState meet AtMost(p)
                case ep @ EP(_, Conditional(p)) ⇒
                    escapeState = escapeState meet p
                    dependees += ep
                case epk ⇒ dependees += epk
            }
        }

        def c(other: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
            p match {
                case p: EscapeProperty if p.isBottom ⇒ Result(fp, escapeState meet p)
                case p @ Conditional(property) ⇒
                    escapeState = escapeState meet property
                    val newEP = EP(other.asInstanceOf[VirtualFormalParameter], p)
                    dependees = dependees.filter(_.e ne other) + newEP
                    IntermediateResult(fp, Conditional(escapeState), dependees, c)
                case p: EscapeProperty ⇒
                    escapeState = escapeState meet p
                    dependees = dependees.filter {
                        _.e ne other
                    }
                    if (dependees.isEmpty) {
                        if (escapeState.isRefinable)
                            RefinableResult(fp, VirtualMethodEscapeProperty(escapeState))
                        else
                            Result(fp, VirtualMethodEscapeProperty(escapeState))
                    } else {
                        IntermediateResult(fp, VirtualMethodEscapeProperty(Conditional(escapeState)), dependees, c)
                    }
                case PropertyIsLazilyComputed ⇒
                    //TODO
                    IntermediateResult(fp, VirtualMethodEscapeProperty(Conditional(escapeState)), dependees, c)
                case _ ⇒ throw new IllegalArgumentException(s"unsupported property $p")
            }
        }

        if (escapeState.isBottom || dependees.isEmpty) {
            if (escapeState.isRefinable) {
                RefinableResult(fp, VirtualMethodEscapeProperty(escapeState))
            } else {
                Result(fp, VirtualMethodEscapeProperty(escapeState))
            }
        } else {
            IntermediateResult(fp, VirtualMethodEscapeProperty(Conditional(escapeState)), dependees, c)
        }
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