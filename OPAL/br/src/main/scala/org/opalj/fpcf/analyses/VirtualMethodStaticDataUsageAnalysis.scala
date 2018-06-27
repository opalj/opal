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

    def determineUsage(dm: DeclaredMethod): PropertyComputationResult = {
        if (!dm.hasSingleDefinedMethod && !dm.hasMultipleDefinedMethods) return Result(dm, VUsesVaryingData);

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
                case FinalEP(_, UsesNoStaticData)     ⇒
                case FinalEP(_, UsesConstantDataOnly) ⇒ maxLevel = UsesConstantDataOnly
                case FinalEP(_, UsesVaryingData)      ⇒ return Result(dm, VUsesVaryingData);
                case ep @ IntermediateEP(_, _, UsesConstantDataOnly) ⇒
                    maxLevel = UsesConstantDataOnly
                    dependees += ep
                case epk ⇒ dependees += epk
            }
        }

        def c(eps: SomeEPS): PropertyComputationResult = {
            dependees = dependees.filter { _.e ne eps.e }

            eps match {
                case FinalEP(_, UsesNoStaticData)     ⇒
                case FinalEP(_, UsesConstantDataOnly) ⇒ maxLevel = UsesConstantDataOnly
                case FinalEP(_, UsesVaryingData)      ⇒ return Result(dm, VUsesVaryingData);
                case ep @ IntermediateEP(_, _, UsesConstantDataOnly) ⇒
                    maxLevel = UsesConstantDataOnly
                    dependees += ep.asInstanceOf[EOptionP[DeclaredMethod, StaticDataUsage]]
                case epk ⇒ dependees += epk.asInstanceOf[EOptionP[DeclaredMethod, StaticDataUsage]]
            }

            if (dependees.isEmpty) {
                Result(dm, maxLevel.aggregatedProperty)
            } else {
                IntermediateResult(
                    dm,
                    VUsesVaryingData,
                    maxLevel.aggregatedProperty,
                    dependees,
                    c
                )
            }
        }

        if (dependees.isEmpty) {
            Result(dm, maxLevel.aggregatedProperty)
        } else {
            IntermediateResult(
                dm,
                VUsesVaryingData,
                maxLevel.aggregatedProperty,
                dependees,
                c
            )
        }
    }

    /** Called when the analysis is scheduled lazily. */
    def doDetermineUsage(e: Entity): PropertyComputationResult = {
        e match {
            case m: DeclaredMethod ⇒ determineUsage(m)
            case _ ⇒ throw new UnknownError(
                "virtual method static data usage is only defined for declared methods"
            )
        }
    }

}

trait VirtualMethodStaticDataUsageAnalysisScheduler extends ComputationSpecification {
    override def derives: Set[PropertyKind] = Set(VirtualMethodStaticDataUsage)

    override def uses: Set[PropertyKind] = Set(StaticDataUsage)
}

object EagerVirtualMethodStaticDataUsageAnalysis
    extends VirtualMethodStaticDataUsageAnalysisScheduler with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualMethodStaticDataUsageAnalysis(project)
        val vms = project.get(DeclaredMethodsKey)
        propertyStore.scheduleEagerComputationsForEntities(vms.declaredMethods)(analysis.determineUsage)
        analysis
    }
}

object LazyVirtualMethodStaticDataUsageAnalysis
    extends VirtualMethodStaticDataUsageAnalysisScheduler with FPCFLazyAnalysisScheduler {

    def startLazily(p: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualMethodStaticDataUsageAnalysis(p)
        ps.registerLazyPropertyComputation(
            VirtualMethodAllocationFreeness.key,
            analysis.doDetermineUsage
        )
        analysis
    }
}
