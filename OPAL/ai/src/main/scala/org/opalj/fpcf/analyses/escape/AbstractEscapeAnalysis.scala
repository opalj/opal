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
import org.opalj.br.Method
import org.opalj.br.AllocationSite
import org.opalj.br.DeclaredMethod
import org.opalj.br.PC
import org.opalj.br.analyses.VirtualFormalParameters
import org.opalj.br.cfg.CFG
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.NoEscape
import org.opalj.tac.DUVar
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.Stmt
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.TACode
import org.opalj.tac.NewArray
import org.opalj.tac.DVar
import org.opalj.tac.CaughtException
import org.opalj.tac.ExprStmt
import org.opalj.tac.Assignment
import org.opalj.tac.New

/**
 * A trait for different implementations of escape analyses. Provides a factory method for the
 * concrete analysis of a single entity and a method to determine the escape information.
 *
 * The control-flow is intended to be: Client calls determineEscape. This method extracts the
 * information for the given entity and calls doDetermineEscape.
 *
 * @see [[AbstractEntityEscapeAnalysis]]
 *
 * @author Florian Kuebler
 */
trait AbstractEscapeAnalysis extends FPCFAnalysis {

    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    /**
     * A factory method for the concrete [[AbstractEntityEscapeAnalysis]]. Every concrete escape
     * analysis must define its corresponding entity analysis.
     */
    protected def entityEscapeAnalysis(
        e:       Entity,
        defSite: ValueOrigin,
        uses:    IntTrieSet,
        code:    Array[Stmt[V]],
        cfg:     CFG,
        m:       DeclaredMethod
    ): AbstractEntityEscapeAnalysis

    /**
     * Creates a new entity escape analysis using [[entityEscapeAnalysis]] and calls
     * [[org.opalj.fpcf.analyses.escape.AbstractEntityEscapeAnalysis.doDetermineEscape]] to
     * compute the escape state.
     */
    protected final def doDetermineEscape(
        e:       Entity,
        defSite: ValueOrigin,
        uses:    IntTrieSet,
        code:    Array[Stmt[V]],
        cfg:     CFG,
        m:       DeclaredMethod
    ): PropertyComputationResult = {
        val analysis = entityEscapeAnalysis(
            e,
            defSite,
            uses,
            code,
            cfg,
            m
        )
        analysis.doDetermineEscape()
    }

    /**
     * Extracts information from the given entity and should call [[doDetermineEscape]] afterwards.
     * For some entities a result might be returned immediately.
     */
    def determineEscape(e: Entity): PropertyComputationResult

    protected[this] final def findUsesOfASAndAnalyze(
        as:    AllocationSite,
        index: PC,
        code:  Array[Stmt[V]],
        cfg:   CFG
    ): PropertyComputationResult = {
        val pc = as.pc
        val m = as.method
        code(index) match {
            case Assignment(`pc`, DVar(_, uses), New(`pc`, _) | NewArray(`pc`, _, _)) ⇒
                doDetermineEscape(as, index, uses, code, cfg, declaredMethods(m))
            case ExprStmt(`pc`, New(`pc`, _) | NewArray(`pc`, _, _)) ⇒
                Result(as, NoEscape)
            case CaughtException(`pc`, _, _) ⇒ findUsesOfASAndAnalyze(as, index + 1, code, cfg)
            case stmt ⇒
                throw new RuntimeException(s"This analysis can't handle entity: $as for $stmt")
        }
    }

    protected[this] val tacaiProvider: (Method) ⇒ TACode[TACMethodParameter, DUVar[(Domain with RecordDefUse)#DomainValue]] = project.get(DefaultTACAIKey)
    protected[this] lazy val virtualFormalParameters: VirtualFormalParameters = propertyStore.context[VirtualFormalParameters]
    protected[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
}
