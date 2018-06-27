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
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.fpcf.properties.ClassifiedImpure
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.CompileTimePure
import org.opalj.fpcf.properties.VirtualMethodPurity.VImpureByAnalysis
import org.opalj.fpcf.properties.VirtualMethodPurity.VImpureByLackOfInformation

/**
 * Determines the aggregated purity for virtual methods.
 *
 * @author Dominik Helm
 */
class VirtualMethodPurityAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val declaredMethods = project.get(DeclaredMethodsKey)

    def determinePurity(dm: DefinedMethod): PropertyComputationResult = {
        if (!dm.hasSingleDefinedMethod && !dm.hasMultipleDefinedMethods)
            return Result(dm, VImpureByLackOfInformation);

        var maxPurity: Purity = CompileTimePure
        var dependees: Set[EOptionP[DeclaredMethod, Purity]] = Set.empty

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
            propertyStore(declaredMethods(method), Purity.key) match {
                case eps @ EPS(_, _, ub) ⇒
                    maxPurity = maxPurity meet ub
                    if (eps.isRefinable) dependees += eps
                case epk ⇒ dependees += epk
            }
        }

        def c(eps: SomeEPS): PropertyComputationResult = {
            dependees = dependees.filter { _.e ne eps.e }
            maxPurity = maxPurity meet eps.ub.asInstanceOf[Purity]
            if (eps.isRefinable) {
                dependees += eps.asInstanceOf[EOptionP[DeclaredMethod, Purity]]
            }

            if (dependees.isEmpty || maxPurity.isInstanceOf[ClassifiedImpure]) {
                Result(dm, maxPurity.aggregatedProperty)
            } else {
                IntermediateResult(dm, VImpureByAnalysis, maxPurity.aggregatedProperty, dependees, c)
            }
        }

        if (dependees.isEmpty || maxPurity.isInstanceOf[ClassifiedImpure]) {
            Result(dm, maxPurity.aggregatedProperty)
        } else {
            IntermediateResult(dm, VImpureByAnalysis, maxPurity.aggregatedProperty, dependees, c)
        }
    }

    /** Called when the analysis is scheduled lazily. */
    def doDeterminePurity(e: Entity): PropertyComputationResult = {
        e match {
            case m: DefinedMethod ⇒ determinePurity(m)
            case _ ⇒ throw new UnknownError(
                "virtual method purity is only defined for defined methods"
            )
        }
    }

}

trait VirtualMethodPurityAnalysisScheduler extends ComputationSpecification {
    override def derives: Set[PropertyKind] = Set(VirtualMethodPurity)

    override def uses: Set[PropertyKind] = Set(Purity)
}

object EagerVirtualMethodPurityAnalysis extends VirtualMethodPurityAnalysisScheduler with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualMethodPurityAnalysis(project)
        val vms = project.get(DeclaredMethodsKey)
        val configuredPurity = project.get(ConfiguredPurityKey)
        propertyStore.scheduleEagerComputationsForEntities(
            vms.declaredMethods.filter { dm ⇒
                !configuredPurity.wasSet(dm) && dm.isInstanceOf[DefinedMethod]
            }.map(_.asInstanceOf[DefinedMethod])
        )(analysis.determinePurity)
        analysis
    }
}

object LazyVirtualMethodPurityAnalysis extends VirtualMethodPurityAnalysisScheduler with FPCFLazyAnalysisScheduler {
    def startLazily(p: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualMethodPurityAnalysis(p)
        ps.registerLazyPropertyComputation(VirtualMethodPurity.key, analysis.doDeterminePurity)
        analysis
    }
}
