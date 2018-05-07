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

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.VirtualDeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.analyses.VirtualFormalParametersKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.cfg.CFG
import org.opalj.fpcf.properties._
import org.opalj.ai.DefinitionSite
import org.opalj.ai.DefinitionSitesKey
import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.ai.domain.RecordDefUse
import org.opalj.tac.DUVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.Stmt
import org.opalj.tac.TACode
import org.opalj.tac.TACStmts

class InterProceduralEscapeAnalysisContext(
        val entity:                  Entity,
        val defSite:                 ValueOrigin,
        val targetMethod:            DeclaredMethod,
        val uses:                    IntTrieSet,
        val code:                    Array[Stmt[V]],
        val cfg:                     CFG[Stmt[V], TACStmts[V]],
        val declaredMethods:         DeclaredMethods,
        val virtualFormalParameters: VirtualFormalParameters,
        val project:                 SomeProject,
        val propertyStore:           PropertyStore,
        val isMethodOverridable:     Method ⇒ Answer
) extends AbstractEscapeAnalysisContext
    with PropertyStoreContainer
    with IsMethodOverridableContainer
    with VirtualFormalParametersContainer
    with DeclaredMethodsContainer
    with CFGContainer

class InterProceduralEscapeAnalysisState
    extends AbstractEscapeAnalysisState with DependeeCache with ReturnValueUseSites

/**
 * A flow-sensitive inter-procedural escape analysis.
 *
 * @author Florian Kuebler
 */
class InterProceduralEscapeAnalysis private[analyses] (
        final val project: SomeProject
) extends DefaultEscapeAnalysis
    with AbstractInterProceduralEscapeAnalysis
    with ConstructorSensitiveEscapeAnalysis
    with ConfigurationBasedConstructorEscapeAnalysis
    with SimpleFieldAwareEscapeAnalysis
    with ExceptionAwareEscapeAnalysis {

    override type AnalysisContext = InterProceduralEscapeAnalysisContext
    type AnalysisState = InterProceduralEscapeAnalysisState

    private[this] val isMethodOverridable: Method ⇒ Answer = project.get(IsOverridableMethodKey)

    /**
     * Determine whether the given entity ([[org.opalj.ai.DefinitionSite]] or
     * [[org.opalj.br.analyses.VirtualFormalParameter]]) escapes its method.
     */
    def determineEscape(e: Entity): PropertyComputationResult = {
        e match {
            case as: DefinitionSite         ⇒ determineEscapeOfDS(as)

            case fp: VirtualFormalParameter ⇒ determineEscapeOfFP(fp)

            case e ⇒
                throw new IllegalArgumentException(s"can't handle entity $e")
        }
    }

    override def determineEscapeOfFP(fp: VirtualFormalParameter): PropertyComputationResult = {
        fp match {
            case VirtualFormalParameter(DefinedMethod(_, m), _) if m.body.isEmpty ⇒
                //TODO IntermediateResult(fp, GlobalEscape, AtMost(NoEscape), Seq.empty, (_) ⇒ throw new RuntimeException())
                Result(fp, AtMost(NoEscape))
            case VirtualFormalParameter(dm @ DefinedMethod(_, m), -1) ⇒
                val TACode(params, code, cfg, _, _) = project.get(DefaultTACAIKey)(m)
                val param = params.thisParameter
                val ctx = createContext(fp, param.origin, dm, param.useSites, code, cfg)
                doDetermineEscape(ctx, createState)

            // parameters of base types are not considered
            case VirtualFormalParameter(m, i) if m.descriptor.parameterType(-i - 2).isBaseType ⇒
                //TODO IntermediateResult(fp, GlobalEscape, AtMost(NoEscape), Seq.empty, (_) ⇒ throw new RuntimeException())
                Result(fp, AtMost(NoEscape))
            case VirtualFormalParameter(dm @ DefinedMethod(_, m), i) ⇒
                val TACode(params, code, cfg, _, _) = project.get(DefaultTACAIKey)(m)
                val param = params.parameter(i)
                val ctx = createContext(fp, param.origin, dm, param.useSites, code, cfg)
                doDetermineEscape(ctx, createState)
            case VirtualFormalParameter(VirtualDeclaredMethod(_, _, _), _) ⇒
                throw new IllegalArgumentException()
        }
    }

    override def createContext(
        entity:       Entity,
        defSite:      ValueOrigin,
        targetMethod: DeclaredMethod,
        uses:         IntTrieSet,
        code:         Array[Stmt[V]],
        cfg:          CFG[Stmt[V], TACStmts[V]]
    ): InterProceduralEscapeAnalysisContext = new InterProceduralEscapeAnalysisContext(
        entity,
        defSite,
        targetMethod,
        uses,
        code,
        cfg,
        declaredMethods,
        virtualFormalParameters,
        project,
        propertyStore,
        isMethodOverridable
    )

    override def createState: InterProceduralEscapeAnalysisState = new InterProceduralEscapeAnalysisState()
}

trait InterProceduralEscapeAnalysisScheduler extends ComputationSpecification {

    override def derives: Set[PropertyKind] = Set(EscapeProperty)

    override def uses: Set[PropertyKind] = Set(VirtualMethodEscapeProperty)
}

object EagerInterProceduralEscapeAnalysis extends InterProceduralEscapeAnalysisScheduler with FPCFEagerAnalysisScheduler {
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {

        val analysis = new InterProceduralEscapeAnalysis(project)
        val fps = project.get(VirtualFormalParametersKey).virtualFormalParameters
        val ass = project.get(DefinitionSitesKey).getAllocationSites

        propertyStore.scheduleForEntities(fps ++ ass)(analysis.determineEscape)
        analysis
    }
}

object LazyInterProceduralEscapeAnalysis extends InterProceduralEscapeAnalysisScheduler with FPCFLazyAnalysisScheduler {
    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new InterProceduralEscapeAnalysis(project)

        propertyStore.registerLazyPropertyComputation(EscapeProperty.key, analysis.determineEscape)
        analysis
    }
}
