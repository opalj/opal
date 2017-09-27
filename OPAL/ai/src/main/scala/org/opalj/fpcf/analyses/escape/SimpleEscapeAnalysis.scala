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

import org.opalj.ai.ValueOrigin
import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.tac.TACMethodParameter
import org.opalj.br.Method
import org.opalj.br.AllocationSite
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.FormalParameter
import org.opalj.br.analyses.FormalParametersKey
import org.opalj.br.analyses.AllocationSitesKey
import org.opalj.tac.Stmt
import org.opalj.collection.immutable.IntArraySet
import org.opalj.fpcf.properties.MaybeNoEscape
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.tac.Parameters
import org.opalj.tac.DVar
import org.opalj.tac.TACode
import org.opalj.tac.ExprStmt
import org.opalj.tac.Assignment
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.DUVar

/**
 * A simple escape analysis that can handle [[AllocationSite]]s and [[FormalParameter]]s (the this
 * parameter of a constructor). All other [[FormalParameter]]s are marked as [[MaybeNoEscape]].
 * It uses [[SimpleEntityEscapeAnalysis]] to handle a specific entity.
 *
 *
 * @author Florian Kuebler
 */
class SimpleEscapeAnalysis( final val project: SomeProject) extends AbstractEscapeAnalysis {

    /**
     * Creates a [[SimpleEntityEscapeAnalysis]] for the analysis.
     */
    override def entityEscapeAnalysis(
        e:       Entity,
        defSite: ValueOrigin,
        uses:    IntArraySet,
        code:    Array[Stmt[DUVar[(Domain with RecordDefUse)#DomainValue]]],
        params:  Parameters[TACMethodParameter],
        m:       Method
    ): AbstractEntityEscapeAnalysis =
        new SimpleEntityEscapeAnalysis(e, defSite, uses, code, params, m, propertyStore, project)

    /**
     * Calls [[doDetermineEscape]] with the definition site, the use sites, the [[TACode]], the
     * [[Parameters]] and the method in which the entity is defined.
     * Allocation sites that are part of dead code are immediately returned as [[NoEscape]].
     * [[FormalParameter]]s that are not the this local of a constructor are flagged as
     * [[MaybeNoEscape]].
     *
     * @param e The entity whose escape state has to be determined.
     */
    override def determineEscape(e: Entity): PropertyComputationResult = {
        e match {
            // for allocation sites, find the code containing the allocation site
            case as @ AllocationSite(m, pc, _) ⇒
                val TACode(params, code, _, _, _) = tacai(m)

                val index = code indexWhere { stmt ⇒ stmt.pc == pc }

                // check if the allocation site is not dead
                if (index != -1)
                    code(index) match {
                        case Assignment(`pc`, DVar(_, uses), New(`pc`, _) | NewArray(`pc`, _, _)) ⇒
                            doDetermineEscape(as, index, uses, code, params, m)
                        case ExprStmt(`pc`, NewArray(`pc`, _, _)) ⇒
                            ImmediateResult(e, NoEscape)
                        case stmt ⇒
                            throw new RuntimeException(s"This analysis can't handle entity: $e for $stmt")
                    }
                else /* the allocation site is part of dead code */ ImmediateResult(e, NoEscape)
            case FormalParameter(m, _) if m.body.isEmpty ⇒ ImmediateResult(e, MaybeNoEscape)
            case FormalParameter(m, -1) if m.name == "<init>" ⇒
                val TACode(params, code, _, _, _) = tacai(m)
                val useSites = params.thisParameter.useSites
                doDetermineEscape(e, -1, useSites, code, params, m)
            case FormalParameter(_, _) ⇒ ImmediateResult(e, MaybeNoEscape)
        }
    }
}

/**
 * A companion object used to start the analysis.
 */
object SimpleEscapeAnalysis extends FPCFAnalysisRunner {
    
    override def derivedProperties: Set[PropertyKind] = Set(EscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set.empty

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new SimpleEscapeAnalysis(project)
        val fps = FormalParametersKey.entityDerivationFunction(project)._1
        val ass = AllocationSitesKey.entityDerivationFunction(project)._1
        propertyStore.scheduleForEntities(fps ++ ass)(analysis.determineEscape)
        analysis
    }
}
