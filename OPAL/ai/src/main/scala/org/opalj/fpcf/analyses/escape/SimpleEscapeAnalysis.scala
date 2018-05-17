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
import org.opalj.ai.common.DefinitionSitesKey
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.tac.Stmt
import org.opalj.tac.TACStmts
import org.opalj.tac.TACode

class SimpleEscapeAnalysisContext(
        val entity:                  Entity,
        val defSite:                 ValueOrigin,
        val targetMethod:            Method,
        val uses:                    IntTrieSet,
        val code:                    Array[Stmt[V]],
        val cfg:                     CFG[Stmt[V], TACStmts[V]],
        val declaredMethods:         DeclaredMethods,
        val virtualFormalParameters: VirtualFormalParameters,
        val project:                 SomeProject,
        val propertyStore:           PropertyStore
) extends AbstractEscapeAnalysisContext
    with PropertyStoreContainer
    with VirtualFormalParametersContainer
    with DeclaredMethodsContainer
    with CFGContainer

/**
 * A simple escape analysis that can handle [[org.opalj.ai.DefinitionSiteLike]]s and
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s (the this parameter of a constructor). All other
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s are marked as
 * [[org.opalj.fpcf.properties.AtMost]]([[org.opalj.fpcf.properties.NoEscape]]).
 *
 *
 *
 *
 * @author Florian Kuebler
 */
class SimpleEscapeAnalysis( final val project: SomeProject)
    extends DefaultEscapeAnalysis
    with ConstructorSensitiveEscapeAnalysis
    with ConfigurationBasedConstructorEscapeAnalysis
    with SimpleFieldAwareEscapeAnalysis
    with ExceptionAwareEscapeAnalysis {

    override type AnalysisContext = SimpleEscapeAnalysisContext
    override type AnalysisState = AbstractEscapeAnalysisState

    override def determineEscapeOfFP(fp: VirtualFormalParameter): PropertyComputationResult = {
        fp match {
            case VirtualFormalParameter(DefinedMethod(_, m), _) if m.body.isEmpty ⇒
                Result(fp, AtMost(NoEscape))
            case VirtualFormalParameter(DefinedMethod(_, m), -1) if m.isInitializer ⇒
                val TACode(params, code, _, cfg, _, _) = tacaiProvider(m)
                val useSites = params.thisParameter.useSites
                val ctx = createContext(fp, -1, m, useSites, code, cfg)
                doDetermineEscape(ctx, createState)
            case VirtualFormalParameter(_, _) ⇒
                //TODO IntermediateResult(fp, GlobalEscape, AtMost(NoEscape), Seq.empty, (_) ⇒ throw new RuntimeException())
                Result(fp, AtMost(NoEscape))
        }
    }

    override def createContext(
        entity:       Entity,
        defSite:      ValueOrigin,
        targetMethod: Method,
        uses:         IntTrieSet,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]]
    ): SimpleEscapeAnalysisContext = new SimpleEscapeAnalysisContext(
        entity,
        defSite,
        targetMethod,
        uses,
        code,
        cfg,
        declaredMethods,
        virtualFormalParameters,
        project,
        propertyStore
    )

    override def createState: AbstractEscapeAnalysisState = new AbstractEscapeAnalysisState {}
}

trait SimpleEscapeAnalysisScheduler extends ComputationSpecification {
    override def derives: Set[PropertyKind] = Set(EscapeProperty)

    override def uses: Set[PropertyKind] = Set.empty
}

/**
 * A companion object used to start the analysis.
 */
object EagerSimpleEscapeAnalysis
    extends SimpleEscapeAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val fps = project.get(VirtualFormalParametersKey).virtualFormalParameters
        val ass = project.get(DefinitionSitesKey).getAllocationSites
        val analysis = new SimpleEscapeAnalysis(project)
        propertyStore.scheduleEagerComputationsForEntities(fps ++ ass)(analysis.determineEscape)
        analysis
    }
}

object LazySimpleEscapeAnalysis
    extends SimpleEscapeAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new SimpleEscapeAnalysis(project)
        propertyStore.registerLazyPropertyComputation(EscapeProperty.key, analysis.determineEscape)
        analysis
    }
}
