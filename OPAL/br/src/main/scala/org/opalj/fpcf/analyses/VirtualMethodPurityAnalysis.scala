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
import org.opalj.fpcf.properties.PureWithoutAllocations
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.CPureWithoutAllocations
import org.opalj.fpcf.properties.ClassifiedImpure

/**
 * Determines the aggregated purity for virtual methods.
 *
 * @author Dominik Helm
 */
class VirtualMethodPurityAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {

    val declaredMethods = project.get(DeclaredMethodsKey)

    def determinePurity(dm: DefinedMethod): PropertyComputationResult = {
        val m = dm.definedMethod
        var maxPurity: Purity = PureWithoutAllocations
        var dependees: Set[EOptionP[DefinedMethod, Purity]] = Set.empty

        val methods = project.virtualCall(
            m.classFile.thisType.packageName, dm.declaringClassType, m.name, m.descriptor
        )
        for (method ← methods) {
            propertyStore(declaredMethods(method), Purity.key) match {
                case ep @ EP(_, p) ⇒
                    maxPurity = maxPurity combine p
                    if (p.isConditional) dependees += ep
                case epk ⇒ dependees += epk
            }
        }

        def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
            dependees = dependees.filter { _.e ne e }
            maxPurity = maxPurity combine p.asInstanceOf[Purity]
            if (p.asInstanceOf[Purity].isConditional) {
                assert(!ut.isFinalUpdate)
                dependees += EP(e.asInstanceOf[DefinedMethod], p.asInstanceOf[Purity])
            }

            if (dependees.isEmpty || maxPurity.isInstanceOf[ClassifiedImpure]) {
                //println(s"Computing VMPurity for $vm : $maxPurity")
                Result(dm, VirtualMethodPurity(maxPurity.unconditional))
            } else {
                IntermediateResult(dm, VirtualMethodPurity(maxPurity), dependees, c)
            }
        }

        if (dependees.isEmpty || maxPurity.isInstanceOf[ClassifiedImpure]) {
            //println(s"Computing VMPurity for $vm : $maxPurity")
            Result(dm, VirtualMethodPurity(maxPurity.unconditional))
        } else {
            maxPurity = maxPurity combine CPureWithoutAllocations
            IntermediateResult(dm, VirtualMethodPurity(maxPurity), dependees, c)
        }
    }

    /** Called when the analysis is scheduled lazily. */
    def doDeterminePurity(e: Entity): PropertyComputationResult = {
        e match {
            case m: DefinedMethod ⇒ determinePurity(m)
            case e ⇒ throw new UnknownError(
                "virtual method purity is only defined for defined methods"
            )
        }
    }

}

object VirtualMethodPurityAnalysis extends FPCFAnalysisScheduler {

    override def derivedProperties: Set[PropertyKind] = Set(VirtualMethodPurity)

    override def usedProperties: Set[PropertyKind] = Set(Purity)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualMethodPurityAnalysis(project)
        val vms = project.get(DeclaredMethodsKey)
        propertyStore.scheduleForEntities(vms.declaredMethods)(analysis.doDeterminePurity)
        analysis
    }

    def startLazily(p: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new VirtualMethodPurityAnalysis(p)
        ps.scheduleLazyPropertyComputation(Purity.key, analysis.doDeterminePurity)
        analysis
    }
}
