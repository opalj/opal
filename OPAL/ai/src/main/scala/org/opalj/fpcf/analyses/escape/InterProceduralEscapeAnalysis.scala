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
package escape

import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.br.AllocationSite
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.VirtualDeclaredMethod
import org.opalj.br.analyses.AllocationSites
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
//import org.opalj.br.analyses.FormalParameters
//import org.opalj.br.analyses.AllocationSites
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties._
import org.opalj.tac.DUVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.Stmt
import org.opalj.tac.TACode
//import org.opalj.util.PerformanceEvaluation.time
//import org.opalj.log.OPALLogger.info

/**
 * A very simple flow-sensitive inter-procedural escape analysis.
 *
 * @author Florian Kuebler
 */
class InterProceduralEscapeAnalysis private (
        final val project: SomeProject
) extends AbstractEscapeAnalysis {

    override def entityEscapeAnalysis(
        e:       Entity,
        defSite: ValueOrigin,
        uses:    IntTrieSet,
        code:    Array[Stmt[V]],
        cfg:     CFG,
        m:       DeclaredMethod
    ): AbstractEntityEscapeAnalysis =
        new InterProceduralEntityEscapeAnalysis(
            e,
            defSite,
            uses,
            code,
            cfg,
            declaredMethods,
            virtualFormalParameters,
            m,
            propertyStore,
            project
        )

    /**
     * Determine whether the given entity ([[AllocationSite]] or [[org.opalj.br.analyses.VirtualFormalParameter]]) escapes
     * its method.
     */
    def determineEscape(e: Entity): PropertyComputationResult = {
        e match {
            case as @ AllocationSite(m, pc, _) ⇒
                val TACode(_, code, cfg, _, _) = tacaiProvider(m)

                val index = code indexWhere { stmt ⇒ stmt.pc == pc }

                if (index != -1)
                    findUsesOfASAndAnalyze(as, index, code, cfg)
                else /* the allocation site is part of dead code */ Result(e, NoEscape)

            case VirtualFormalParameter(DefinedMethod(_, m), _) if m.body.isEmpty ⇒ RefinableResult(e, AtMost(NoEscape))
            case VirtualFormalParameter(dm @ DefinedMethod(_, m), -1) ⇒
                val TACode(params, code, cfg, _, _) = project.get(DefaultTACAIKey)(m)
                val param = params.thisParameter
                doDetermineEscape(e, param.origin, param.useSites, code, cfg, dm)

            // parameters of base types are not considered
            case VirtualFormalParameter(m, i) if m.descriptor.parameterType(-i - 2).isBaseType ⇒
                RefinableResult(e, AtMost(NoEscape))
            case VirtualFormalParameter(dm @ DefinedMethod(_, m), i) ⇒
                val TACode(params, code, cfg, _, _) = project.get(DefaultTACAIKey)(m)
                val param = params.parameter(i)
                doDetermineEscape(e, param.origin, param.useSites, code, cfg, dm)
            case VirtualFormalParameter(VirtualDeclaredMethod(_, _, _), _) ⇒
                throw new IllegalArgumentException()
        }
    }
}

object InterProceduralEscapeAnalysis extends FPCFAnalysisScheduler {

    type V = DUVar[Domain#DomainValue]

    override def derivedProperties: Set[PropertyKind] = Set(EscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set.empty

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        /*implicit val logContext = project.logContext
        time {
            SimpleEscapeAnalysis.start(project)
            propertyStore.waitOnPropertyComputationCompletion(
                resolveCycles = false,
                useFallbacksForIncomputableProperties = false
            )
        } { t ⇒ info("progress", s"simple escape analysis took ${t.toSeconds}") }*/

        val analysis = new InterProceduralEscapeAnalysis(project)

        //val fps = propertyStore.context[FormalParameters].formalParameters.filter(propertyStore(_, EscapeProperty.key).p.isRefineable)
        //val ass = propertyStore.context[AllocationSites].allocationSites.filter(propertyStore(_, EscapeProperty.key).p.isRefineable)
        val fps = propertyStore.context[VirtualFormalParameters].virtualFormalParameters
        val ass = propertyStore.context[AllocationSites].allocationSites

        propertyStore.scheduleForEntities(fps ++ ass)(analysis.determineEscape)
        analysis
    }

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    override protected[fpcf] def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        VirtualCallAggregatingEscapeAnalysis.startLazily(project)
        val analysis = new InterProceduralEscapeAnalysis(project)

        propertyStore.scheduleLazyPropertyComputation(EscapeProperty.key, analysis.determineEscape)
        analysis
    }
}
