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

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VNoFreshReturnValue
import org.opalj.fpcf.properties.VPrimitiveReturnValue
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness

/**
 * An analysis that aggregates whether the return value for all possible methods represented by a
 * given [[org.opalj.br.DeclaredMethod]] are always freshly allocated.
 *
 * @author Florian Kuebler
 */
class VirtualReturnValueFreshnessAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def determineFreshness(m: DeclaredMethod): PropertyComputationResult = {
        if (m.descriptor.returnType.isBaseType || m.descriptor.returnType.isVoidType) {
            return Result(m, VPrimitiveReturnValue)
        }

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty

        if (m.declaringClassType.isArrayType) {
            throw new NotImplementedError()
        }

        val methods = project.virtualCall(
            m.declaringClassType.asObjectType.packageName,
            m.declaringClassType,
            m.name,
            m.descriptor
        )

        var temporary: VirtualMethodReturnValueFreshness = VFreshReturnValue

        for (method ← methods) {
            val rvf = propertyStore(declaredMethods(method), ReturnValueFreshness.key)
            handleReturnValueFreshness(rvf).foreach(return _)
        }

        def handleReturnValueFreshness(
            eOptionP: EOptionP[DeclaredMethod, ReturnValueFreshness]
        ): Option[PropertyComputationResult] = eOptionP match {
            case FinalEP(_, NoFreshReturnValue) ⇒
                Some(Result(m, VNoFreshReturnValue))

            case FinalEP(_, PrimitiveReturnValue) ⇒
                throw new RuntimeException("unexpected property")

            case ep @ EPS(_, _, p) ⇒
                temporary = temporary meet p.asVirtualMethodReturnValueFreshness
                if (ep.isRefinable)
                    dependees += ep
                None

            case epk ⇒
                dependees += epk
                None
        }

        def returnResult(): PropertyComputationResult = {
            if (dependees.isEmpty)
                Result(m, temporary)
            else
                IntermediateResult(m, VNoFreshReturnValue, temporary, dependees, c)
        }

        def c(someEPS: SomeEPS): PropertyComputationResult = {

            dependees = dependees.filter(_.e ne someEPS.e)

            handleReturnValueFreshness(
                someEPS.asInstanceOf[EOptionP[DeclaredMethod, ReturnValueFreshness]]
            ).foreach(return _)

            returnResult()
        }

        returnResult()
    }

}

trait VirtualReturnValueFreshnessAnalysisScheduler extends ComputationSpecification {
    override def derives: Set[PropertyKind] = Set(VirtualMethodReturnValueFreshness)

    override def uses: Set[PropertyKind] = Set(ReturnValueFreshness)
}

object EagerVirtualReturnValueFreshnessAnalysis extends VirtualReturnValueFreshnessAnalysisScheduler with FPCFEagerAnalysisScheduler {
    override def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val declaredMethods = project.get(DeclaredMethodsKey).declaredMethods
        val analysis = new VirtualReturnValueFreshnessAnalysis(project)
        propertyStore.scheduleEagerComputationsForEntities(declaredMethods)(analysis.determineFreshness)
        analysis
    }
}

object LazyVirtualReturnValueFreshnessAnalysis extends VirtualCallAggregatingEscapeAnalysisScheduler with FPCFLazyAnalysisScheduler {
    override def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualReturnValueFreshnessAnalysis(project)
        propertyStore.registerLazyPropertyComputation(
            VirtualMethodReturnValueFreshness.key,
            analysis.determineFreshness
        )
        analysis
    }
}
