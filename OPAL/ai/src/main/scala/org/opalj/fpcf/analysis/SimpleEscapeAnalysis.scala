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
package analysis

import org.opalj.ai.Domain
import org.opalj.br.analyses.SomeProject
import org.opalj.br.{AllocationSite, ObjectType}
import org.opalj.collection.immutable.IntSet
import org.opalj.fpcf.properties._
import org.opalj.tac._

class SimpleEscapeAnalysis private(final val project: SomeProject) extends FPCFAnalysis {
    type V = DUVar[Domain#DomainValue]

    private def checkParams(params: Seq[Expr[V]], defSite: Int) = {
        params.exists { case UVar(_, defSites) ⇒ defSites.contains(defSite) }
    }

    private def determineEscapeStmt(stmt: Stmt[V], e: AllocationSite): Option[PropertyComputationResult] = {
        val defSite = e.pc
        stmt match {
            case PutStatic(_, _, _, _, UVar(_, defSites)) if defSites.contains(defSite) ⇒
                Some(ImmediateResult(e, GlobalEscape))
            case PutField(_, _, _, _, _, UVar(_, defSites)) if defSites.contains(defSite) ⇒
                Some(Result(e, GlobalEscape))
            case ReturnValue(_, UVar(_, defSites)) if defSites.contains(defSite) ⇒
                Some(Result(e, GlobalEscape))
            case ArrayStore(_, _, _, UVar(_, defSites)) if defSites.contains(defSite) ⇒
                Some(Result(e, GlobalEscape))
            case Throw(_, UVar(_, defSites)) if defSites.contains(defSite) ⇒
                Some(Result(e, GlobalEscape))
            case StaticMethodCall(_, _, _, _, _, params) if checkParams(params, defSite) ⇒
                Some(Result(e, GlobalEscape))
            case VirtualMethodCall(_, _, _, _, _, UVar(_, defSites), params)
                if defSites.contains(defSite) || checkParams(params, defSite) ⇒ Some(Result(e, GlobalEscape))
            // TODO: base local constructor chain!
            case NonVirtualMethodCall(_, ObjectType.Object, _, "<init>", _, UVar(_, defSites), _)
                if defSites.contains(defSite) => None
            case NonVirtualMethodCall(_, _, _, _, _, _, params) if checkParams(params, defSite) ⇒
                Some(Result(e, GlobalEscape))
            case FailingStatement(_, failingStmt) ⇒
                determineEscapeStmt(failingStmt, e)
            case Assignment(_, _, right) ⇒ right match {
                //TODO chain
                case NonVirtualFunctionCall(_, ObjectType.Object, _, "<init>", _, UVar(_, defSites), _)
                    if defSites.contains(defSite) => None
                case NonVirtualFunctionCall(_, _, _, _, _, UVar(_, defSites), params)
                    if defSites.contains(defSite) || checkParams(params, defSite) ⇒ Some(Result(e, GlobalEscape))
                case VirtualFunctionCall(_, _, _, _, _, UVar(_, defSites), params)
                    if defSites.contains(defSite) || checkParams(params, defSite) ⇒ Some(Result(e, GlobalEscape))
                case StaticFunctionCall(_, _, _, _, _, params) if checkParams(params, defSite) ⇒
                    Some(Result(e, GlobalEscape))
                //TODO
                //throw new RuntimeException("Requires Invokedynamic resolution (see java8LambdaExpressions...")
                case Invokedynamic(_, _, _, _, params) if checkParams(params, defSite) =>
                    Some(Result(e, GlobalEscape))

                // TODO on standard javac this won't happen
                case Checkcast(_, UVar(_, defSites), _) if defSites.contains(defSite) ⇒
                    throw new RuntimeException("Not yet implemented")
                case _ ⇒ None
            }
            case _ ⇒ None
        }
    }

    private def doDetermineEscape(e: AllocationSite, uses: IntSet, code: Array[Stmt[V]]): PropertyComputationResult = {
        for (use <- uses) {
            determineEscapeStmt(code(use), e).map(return _)
        }
        Result(e, NoEscape)
    }

    def determineEscape(e: AllocationSite): PropertyComputationResult = {
        val TACode(code, _, _, _) = project.get(DefaultTACAIKey)(e.method)

        val allocation = code.find(stmt => stmt.pc == e.pc).get

        allocation match {
            case Assignment(e.pc, DVar(_, uses), New(e.pc, _)) =>
                doDetermineEscape(e, uses, code)
            case Assignment(e.pc, DVar(_, uses), NewArray(e.pc, _, _)) =>
                doDetermineEscape(e, uses, code)
            case stmt => throw new RuntimeException(s"This analysis can't handle entity: $e for $stmt")
        }

    }
}

object SimpleEscapeAnalysis extends FPCFAnalysisRunner {
    type V = DUVar[Domain#DomainValue]

    def entitySelector: PartialFunction[Entity, AllocationSite] = {
        case as: AllocationSite ⇒ as
    }

    override def derivedProperties: Set[PropertyKind] = Set(EscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set.empty

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new SimpleEscapeAnalysis(project)
        propertyStore <||< (entitySelector, analysis.determineEscape)
        analysis
    }
}

