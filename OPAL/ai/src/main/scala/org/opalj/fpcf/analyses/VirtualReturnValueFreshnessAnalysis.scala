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

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.ConditionalFreshReturnValue
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.VConditionalFreshReturnValue
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VNoFreshReturnValue
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness
import org.opalj.tac.DUVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode

/**
  * TODO
  * @author Florian Kuebler
  */
class VirtualReturnValueFreshnessAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val tacaiProvider: (Method) ⇒ TACode[TACMethodParameter, DUVar[(Domain with RecordDefUse)#DomainValue]] = project.get(DefaultTACAIKey)
    private[this] val declaredMethods: DeclaredMethods = propertyStore.context[DeclaredMethods]

    def determineFreshness(m: DeclaredMethod): PropertyComputationResult = {

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty

        if (m.declaringClassType.isArrayType) {
            throw new NotImplementedError()
        }
        val methods = project.virtualCall(
            m.declaringClassType.asObjectType.packageName, //TODO
            m.declaringClassType, //TODO
            m.name,
            m.descriptor
        )

        for (method ← methods) {
            propertyStore(declaredMethods(method), ReturnValueFreshness.key) {
                case EP(_, NoFreshReturnValue) ⇒ return Result(m, VNoFreshReturnValue)
                case EP(_, FreshReturnValue)   ⇒
                case ep @ EP(_, ConditionalFreshReturnValue) ⇒
                    dependees += ep
            }
        }

        def c(other: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
            p match {
                case NoFreshReturnValue ⇒ return Result(m, VNoFreshReturnValue)
                case FreshReturnValue ⇒
                    dependees = dependees.filter(_.e ne other)
                    if (dependees.isEmpty)
                        Result(m, VFreshReturnValue)
                    else
                        IntermediateResult(m, VConditionalFreshReturnValue, dependees, c)
                case ConditionalFreshReturnValue ⇒
                    val newEP = EP(other, p)
                    dependees = dependees.filter(_.e ne other) + newEP
                    IntermediateResult(m, VConditionalFreshReturnValue, dependees, c)
            }
        }

        if (dependees.isEmpty)
            Result(m, VFreshReturnValue)
        else
            IntermediateResult(m, VConditionalFreshReturnValue, dependees, c)
    }
}

object VirtualReturnValueFreshnessAnalysis extends FPCFAnalysisScheduler {
    override def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val declaredMethods = propertyStore.context[DeclaredMethods].declaredMethods
        val analysis = new VirtualReturnValueFreshnessAnalysis(project)
        propertyStore.scheduleForEntities(declaredMethods)(analysis.determineFreshness)
        analysis
    }

    override def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualReturnValueFreshnessAnalysis(project)
        propertyStore.scheduleLazyPropertyComputation(
            VirtualMethodReturnValueFreshness.key,
            analysis.determineFreshness
        )
        analysis
    }

    override def derivedProperties: Set[PropertyKind] = Set(VirtualMethodReturnValueFreshness)

    override def usedProperties: Set[PropertyKind] = Set(ReturnValueFreshness)
}
