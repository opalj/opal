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
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.AllocationSites
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.tac.Stmt
/**
 * A simple escape analysis that can handle [[org.opalj.br.AllocationSite]]s and
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s (the this parameter of a constructor). All other
 * [[org.opalj.br.analyses.VirtualFormalParameter]]s are marked as
 * [[org.opalj.fpcf.properties.AtMost(NoEscape)]].
 *
 *
 * @author Florian Kuebler
 */
class FallBackEscapeAnalysis( final val project: SomeProject)
    extends DefaultEscapeAnalysis {

    override type AnalysisContext = AbstractEscapeAnalysisContext
    override type AnalysisState = AbstractEscapeAnalysisState

    /**
     * Extracts information from the given entity and should call [[doDetermineEscape]] afterwards.
     * For some entities a result might be returned immediately.
     */
    override def determineEscape(e: Entity): PropertyComputationResult = ???

    override def determineEscapeOfFP(fp: VirtualFormalParameter): PropertyComputationResult = ???

    override def createContext(
        entityParam:       Entity,
        defSiteParam:      ValueOrigin,
        targetMethodParam: DeclaredMethod,
        usesParam:         IntTrieSet,
        codeParam:         Array[Stmt[V]],
        cfgParam:          CFG
    ): AnalysisContext = new AbstractEscapeAnalysisContext {
        override val defSite: ValueOrigin = defSiteParam
        override val code: Array[Stmt[V]] = codeParam
        override val targetMethod: DeclaredMethod = targetMethodParam
        override val uses: IntTrieSet = usesParam
        override val entity: Entity = entityParam
    }

    override def createState: AnalysisState = new AbstractEscapeAnalysisState {}
}

/**
 * A companion object used to start the analysis.
 */
object FallBackEscapeAnalysis extends FPCFAnalysisScheduler {

    override def derivedProperties: Set[PropertyKind] = Set(EscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set.empty

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new FallBackEscapeAnalysis(project)
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
        val analysis = new FallBackEscapeAnalysis(project)
        propertyStore.scheduleLazyPropertyComputation(EscapeProperty.key, analysis.determineEscape)
        analysis
    }
}
