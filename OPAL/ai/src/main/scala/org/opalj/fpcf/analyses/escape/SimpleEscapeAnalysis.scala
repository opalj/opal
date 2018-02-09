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
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.AllocationSite
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.AllocationSites
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.tac.DUVar
import org.opalj.tac.Stmt
import org.opalj.tac.TACode

/**
 * A simple escape analysis that can handle [[org.opalj.br.AllocationSite]]s and
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s (the this parameter of a constructor). All other
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s are marked as
 * [[org.opalj.fpcf.properties.AtMost(NoEscape)]]. It uses
 * [[org.opalj.fpcf.analyses.escape.SimpleEntityEscapeAnalysis]] to handle a specific entity.
 *
 *
 * @author Florian Kuebler
 */
class SimpleEscapeAnalysis( final val project: SomeProject) extends AbstractEscapeAnalysis {

    /**
     * Creates a [[org.opalj.fpcf.analyses.escape.SimpleEntityEscapeAnalysis]] for the analysis.
     */
    override def entityEscapeAnalysis(
        e:       Entity,
        defSite: ValueOrigin,
        uses:    IntTrieSet,
        code:    Array[Stmt[DUVar[(Domain with RecordDefUse)#DomainValue]]],
        cfg:     CFG,
        m:       DeclaredMethod
    ): AbstractEntityEscapeAnalysis =
        new SimpleEntityEscapeAnalysis(
            e,
            defSite,
            uses,
            code,
            cfg,
            declaredMethods,
            virtualFormalParameters,
            propertyStore,
            project
        )

    /**
     * Calls [[doDetermineEscape]] with the definition site, the use sites, the
     * [[org.opalj.tac.TACode]], the [[org.opalj.tac.Parameters]] and the method in which the entity
     * is defined. Allocation sites that are part of dead code are immediately returned as
     * [[org.opalj.fpcf.properties.NoEscape]].
     * [[org.opalj.br.analyses.VirtualFormalParameter]]s that are not the this local of a constructor are
     * flagged as [[org.opalj.fpcf.properties.AtMost(NoEscape)]].
     *
     * @param e The entity whose escape state has to be determined.
     */
    override def determineEscape(e: Entity): PropertyComputationResult = {
        e match {
            // for allocation sites, find the code containing the allocation site
            case as @ AllocationSite(m, pc, _) ⇒
                val TACode(_, code, cfg, _, _) = tacaiProvider(m)

                val index = code indexWhere { stmt ⇒ stmt.pc == pc }

                // check if the allocation site is not dead
                if (index != -1)
                    findUsesOfASAndAnalyze(as, index, code, cfg)
                else /* the allocation site is part of dead code */ Result(e, NoEscape)
            case VirtualFormalParameter(DefinedMethod(_, m), _) if m.body.isEmpty ⇒ Result(e, AtMost(NoEscape))
            case VirtualFormalParameter(dm @ DefinedMethod(_, m), -1) if m.name == "<init>" ⇒
                val TACode(params, code, cfg, _, _) = tacaiProvider(m)
                val useSites = params.thisParameter.useSites
                doDetermineEscape(e, -1, useSites, code, cfg, dm)
            case VirtualFormalParameter(_, _) ⇒ RefinableResult(e, AtMost(NoEscape))
            case e ⇒
                throw new IllegalArgumentException(s"can't handle entity $e")
        }
    }
}

/**
 * A companion object used to start the analysis.
 */
object SimpleEscapeAnalysis extends FPCFAnalysisScheduler {

    override def derivedProperties: Set[PropertyKind] = Set(EscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set.empty

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new SimpleEscapeAnalysis(project)
        val ass = propertyStore.context[AllocationSites].allocationSites
        val fps = propertyStore.context[VirtualFormalParameters].virtualFormalParameters
        propertyStore.scheduleForEntities(fps ++ ass)(analysis.determineEscape)
        analysis
    }

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new SimpleEscapeAnalysis(project)
        propertyStore.scheduleLazyPropertyComputation(EscapeProperty.key, analysis.determineEscape)
        analysis
    }
}
