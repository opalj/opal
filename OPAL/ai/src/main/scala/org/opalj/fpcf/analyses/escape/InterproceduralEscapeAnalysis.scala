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

import org.opalj.collection.immutable.IntArraySet
import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.ai.AIResult
import org.opalj.br.AllocationSite
import org.opalj.br.ExceptionHandlers
import org.opalj.br.VirtualMethod
import org.opalj.br.analyses.FormalParameter
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.AllocationSitesKey
import org.opalj.br.analyses.FormalParametersKey
import org.opalj.br.cfg.CFG
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties._
import org.opalj.fpcf.PropertyKey.SomeEPKs
import org.opalj.fpcf.properties.EscapeProperty.key
import org.opalj.tac.Assignment
import org.opalj.tac.DUVar
import org.opalj.tac.DVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.ExprStmt
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.Stmt
import org.opalj.tac.TACode
import org.opalj.tac.Parameters
import org.opalj.tac.TACMethodParameter

/**
 * A very simple flow-sensitive inter-procedural escape analysis.
 *
 * @author Florian Kuebler
 */
class InterproceduralEscapeAnalysis private (
        final val project: SomeProject
) extends AbstractEscapeAnalysis {

    override def entityEscapeAnalysis(
        e:        Entity,
        defSite:  ValueOrigin,
        uses:     IntArraySet,
        code:     Array[Stmt[V]],
        params:   Parameters[TACMethodParameter],
        cfg:      CFG,
        handlers: ExceptionHandlers,
        aiResult: AIResult,
        m:        VirtualMethod
    ): AbstractEntityEscapeAnalysis =
        new InterproceduralEntityEscapeAnalysis(e, IntArraySet(defSite), uses, code, params, cfg, handlers, aiResult, m, propertyStore, project)

    /**
     * Determine whether the given entity ([[AllocationSite]] or [[FormalParameter]]) escapes
     * its method.
     */
    def determineEscape(e: Entity): PropertyComputationResult = {
        e match {
            case as @ AllocationSite(m, pc, _) ⇒
                val TACode(params, code, cfg, handlers, _) = tacaiProvider(m)

                val index = code indexWhere { stmt ⇒ stmt.pc == pc }

                if (index != -1)
                    code(index) match {
                        case Assignment(`pc`, DVar(_, uses), New(`pc`, _) | NewArray(`pc`, _, _)) ⇒
                            doDetermineEscape(as, index, uses, code, params, cfg, handlers, aiProvider(m), m.asVirtualMethod)
                        case ExprStmt(`pc`, NewArray(`pc`, _, _)) ⇒
                            ImmediateResult(e, NoEscape)
                        case stmt ⇒
                            throw new RuntimeException(s"This analysis can't handle entity: $e for $stmt")
                    }
                else /* the allocation site is part of dead code */ ImmediateResult(e, NoEscape)

            case FormalParameter(m, _) if m.body.isEmpty ⇒ Result(e, MaybeNoEscape)
            case FormalParameter(m, -1) ⇒
                val TACode(params, code, cfg, handlers, _) = project.get(DefaultTACAIKey)(m)
                val param = params.thisParameter
                doDetermineEscape(e, param.origin, param.useSites, code, params, cfg, handlers, aiProvider(m), m.asVirtualMethod)
            case FormalParameter(m, i) if m.descriptor.parameterType(-i - 2).isBaseType ⇒
                Result(e, MaybeNoEscape)
            case FormalParameter(m, i) ⇒
                val TACode(params, code, cfg, handlers, _) = project.get(DefaultTACAIKey)(m)
                val param = params.parameter(i)
                doDetermineEscape(e, param.origin, param.useSites, code, params, cfg, handlers, aiProvider(m), m.asVirtualMethod)
        }
    }
}

object InterproceduralEscapeAnalysis extends FPCFAnalysisRunner {

    type V = DUVar[Domain#DomainValue]

    def entitySelector(propertyStore: PropertyStore): PartialFunction[Entity, Entity] = {
        case as: AllocationSite /*if !propertyStore(as, EscapeProperty.key).isPropertyFinal */  ⇒ as
        case fp: FormalParameter /*if !propertyStore(fp, EscapeProperty.key).isPropertyFinal */ ⇒ fp
    }

    override def derivedProperties: Set[PropertyKind] = Set(EscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set.empty

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        def cycleResolutionStrategy(ps: PropertyStore, epks: SomeEPKs): Iterable[PropertyComputationResult] = {
            Iterable(
                Result(
                    epks.head.e,
                    epks.foldLeft(NoEscape: EscapeProperty) {
                        (escapeState, epk) ⇒
                            epk match {
                                case EPK(e, `key`) ⇒
                                    ps(e, key).p.atMost meet escapeState
                                case _ ⇒
                                    throw new RuntimeException() //escapeState meet MaybeNoEscape
                            }
                    }
                )
            )
        }

        PropertyKey.updateCycleResolutionStrategy(EscapeProperty.key, cycleResolutionStrategy)

        val analysis = new InterproceduralEscapeAnalysis(project)

        val fps = FormalParametersKey.entityDerivationFunction(project)._1
        val ass = AllocationSitesKey.entityDerivationFunction(project)._1
        propertyStore.scheduleForEntities(fps ++ ass)(analysis.determineEscape)
        analysis
    }
}
