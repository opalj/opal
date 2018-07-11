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
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.fpcf.properties.VirtualMethodAllocationFreeness
import org.opalj.fpcf.properties.AllocationFreeness
import org.opalj.fpcf.properties.MethodWithAllocations
import org.opalj.fpcf.properties.AllocationFreeMethod
import org.opalj.fpcf.properties.VirtualMethodAllocationFreeness.VMethodWithAllocations
import org.opalj.fpcf.properties.VirtualMethodAllocationFreeness.VAllocationFreeMethod

/**
 * Determines the aggregated allocation freeness for virtual methods.
 *
 * @author Dominik Helm
 */
class VirtualMethodAllocationFreenessAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    def determineAllocationFreeness(dm: DeclaredMethod): PropertyComputationResult = {
        if (!dm.hasSingleDefinedMethod && !dm.hasMultipleDefinedMethods)
            return Result(dm, VMethodWithAllocations);

        var dependees: Set[EOptionP[DeclaredMethod, AllocationFreeness]] = Set.empty

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
            propertyStore(declaredMethods(method), AllocationFreeness.key) match {
                case FinalEP(_, AllocationFreeMethod)  ⇒
                case FinalEP(_, MethodWithAllocations) ⇒ return Result(dm, VMethodWithAllocations);
                case epk                               ⇒ dependees += epk
            }
        }

        def c(eps: SomeEPS): PropertyComputationResult = {
            dependees = dependees.filter { _.e ne eps.e }

            eps match {
                case FinalEP(_, AllocationFreeMethod)  ⇒
                case FinalEP(_, MethodWithAllocations) ⇒ return Result(dm, VMethodWithAllocations);
                case epk ⇒
                    dependees += epk.asInstanceOf[EOptionP[DeclaredMethod, AllocationFreeness]]
            }

            if (dependees.isEmpty) {
                Result(dm, VAllocationFreeMethod)
            } else {
                IntermediateResult(dm, VMethodWithAllocations, VAllocationFreeMethod, dependees, c)
            }
        }

        if (dependees.isEmpty) {
            Result(dm, VAllocationFreeMethod)
        } else {
            IntermediateResult(dm, VMethodWithAllocations, VAllocationFreeMethod, dependees, c)
        }
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineAllocationFreeness(e: Entity): PropertyComputationResult = {
        e match {
            case m: DeclaredMethod ⇒ determineAllocationFreeness(m)
            case _ ⇒ throw new UnknownError(
                "virtual method allocation freeness is only defined for declared methods"
            )
        }
    }

}

trait VirtualMethodAllocationFreenessAnalysisScheduler extends ComputationSpecification {

    final override def derives: Set[PropertyKind] = Set(VirtualMethodAllocationFreeness)

    final override def uses: Set[PropertyKind] = Set(AllocationFreeness)

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

object EagerVirtualMethodAllocationFreenessAnalysis
    extends VirtualMethodAllocationFreenessAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodAllocationFreenessAnalysis(p)
        val dms = p.get(DeclaredMethodsKey).declaredMethods
        ps.scheduleEagerComputationsForEntities(dms)(analysis.determineAllocationFreeness)
        analysis
    }
}

object LazyVirtualMethodAllocationFreenessAnalysis
    extends VirtualMethodAllocationFreenessAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    override def startLazily(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new VirtualMethodAllocationFreenessAnalysis(p)
        ps.registerLazyPropertyComputation(
            VirtualMethodAllocationFreeness.key,
            analysis.doDetermineAllocationFreeness
        )
        analysis
    }
}
