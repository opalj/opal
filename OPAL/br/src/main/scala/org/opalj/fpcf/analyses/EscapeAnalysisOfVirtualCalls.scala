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
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.FormalParameters
import org.opalj.br.analyses.FormalParameter
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.MaybeNoEscape

class EscapeAnalysisOfVirtualCalls private ( final val project: SomeProject) extends FPCFAnalysis {

    def determineEscape(fp: VirtualFormalParameter): PropertyComputationResult = {
        val m = fp.method
        var escapeState: EscapeProperty = NoEscape
        var dependees: Set[EOptionP[FormalParameter, EscapeProperty]] = Set.empty
        val methods = project.virtualCall(
            /* use the package in which the concrete method context is defined */
            m.target.classFile.thisType.packageName,
            m.classType, m.name, m.descriptor
        )
        val fps = propertyStore.context[FormalParameters]
        for (method ← methods) {
            propertyStore(fps(method)(-1 - fp.origin), EscapeProperty.key) match {
                case EP(_, p) if p.isFinal ⇒ escapeState = escapeState meet p
                case epk                   ⇒ dependees += epk
            }
        }

        def c(e: Entity, p: Property, ut: UserUpdateType): PropertyComputationResult = {
            p match {
                case p: EscapeProperty if p.isBottom ⇒ Result(fp, p)
                case p: EscapeProperty if p.isFinal ⇒
                    escapeState = escapeState meet p
                    dependees = dependees.filter { _.e ne e }
                    if (dependees.isEmpty) {
                        if (escapeState.isRefineable)
                            RefineableResult(fp, escapeState)
                        else
                            Result(fp, escapeState)
                    } else {
                        // there can't be any cycles, so this is sound
                        IntermediateResult(fp, MaybeNoEscape meet escapeState, dependees, c)
                    }
                case p: EscapeProperty if p.isRefineable ⇒ ut match {
                    case IntermediateUpdate ⇒
                        escapeState = escapeState meet p.atMost
                        val newEP = EP(e.asInstanceOf[FormalParameter], p)
                        dependees = dependees.filter(_.e ne e) + newEP
                        IntermediateResult(fp, MaybeNoEscape meet escapeState, dependees, c)
                    case _ ⇒
                        escapeState = escapeState meet p
                        dependees = dependees.filter { _.e ne e }
                        if (dependees.isEmpty) {
                            if (escapeState.isRefineable)
                                RefineableResult(fp, escapeState)
                            else
                                Result(fp, escapeState)
                        } else {
                            IntermediateResult(fp, escapeState, dependees, c)
                        }
                }
            }
        }

        if (escapeState.isBottom || dependees.isEmpty) {
            if (escapeState.isRefineable) {
                RefineableResult(fp, escapeState)
            } else {
                ImmediateResult(fp, escapeState)
            }
        } else {
            IntermediateResult(fp, MaybeNoEscape meet escapeState, dependees, c)
        }
    }

}

object EscapeAnalysisOfVirtualCalls extends FPCFAnalysisRunner {

    override def derivedProperties: Set[PropertyKind] = Set(EscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set(EscapeProperty)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new EscapeAnalysisOfVirtualCalls(project)
        val vfps = project.get(VirtualFormalParametersKey).virtualFormalParameters
        propertyStore.scheduleForEntities(vfps)(analysis.determineEscape)
        analysis
    }
}