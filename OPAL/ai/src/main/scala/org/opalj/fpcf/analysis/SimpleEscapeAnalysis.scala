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
import org.opalj.br.AllocationSite
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntSet
import org.opalj.fpcf.properties._
import org.opalj.tac._

class SimpleEscapeAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {
    type V = DUVar[Domain#DomainValue]

    private def checkValue(value: Expr[V], p2s: IntSet) = {
        value match {
            case UVar(_, defSites) ⇒
                defSites.exists(p2s.contains)
            case _ ⇒ throw new RuntimeException("arg")
        }
    }

    private def checkParams(params: Seq[Expr[V]], p2s: IntSet) = {
        params.exists(value ⇒ checkValue(value, p2s))
    }

    def determineEscapeStmt(stmt: Stmt[V], e: AllocationSite, p2sparam: IntSet): (IntSet, Option[PropertyComputationResult]) = {
        var p2s = p2sparam
        val result: Option[PropertyComputationResult] = stmt match {
            case PutStatic(_, _, _, _, value) if checkValue(value, p2s) ⇒
                Some(ImmediateResult(e, GlobalEscape))
            case PutField(_, _, _, _, _, value) if checkValue(value, p2s) ⇒
                Some(Result(e, GlobalEscape))
            case ReturnValue(_, value) if checkValue(value, p2s) ⇒
                Some(Result(e, GlobalEscape))
            case ArrayStore(_, _, _, value) if checkValue(value, p2s) ⇒
                Some(Result(e, GlobalEscape))
            case Throw(_, exception) if checkValue(exception, p2s) ⇒
                Some(Result(e, GlobalEscape))
            case StaticMethodCall(_, _, _, _, _, params) if checkParams(params, p2s) ⇒
                Some(Result(e, GlobalEscape))
            case VirtualMethodCall(_, _, _, _, _, receiver, params) if checkValue(receiver, p2s) || checkParams(params, p2s) ⇒
                Some(Result(e, GlobalEscape))
            // TODO: base local constructor chain!
            case NonVirtualMethodCall(_, _, _, _, _, _, params) if checkParams(params, p2s) ⇒
                Some(Result(e, GlobalEscape))
            case FailingStatement(_, failingStmt) ⇒
                return determineEscapeStmt(failingStmt, e, p2s)
            case Assignment(_, left, New(e.pc, _)) ⇒
                val dvar = left.asInstanceOf[DVar[_]].definedBy
                p2s = p2s + dvar
                None
            case Assignment(_, _, right) ⇒ right match {
                case NonVirtualFunctionCall(_, _, _, _, _, _, params) if checkParams(params, p2s) ⇒
                    Some(Result(e, GlobalEscape))
                case VirtualFunctionCall(_, _, _, _, _, receiver, params) if checkValue(receiver, p2s) || checkParams(params, p2s) ⇒
                    Some(Result(e, GlobalEscape))
                case StaticFunctionCall(_, _, _, _, _, params) if checkParams(params, p2s) ⇒
                    Some(Result(e, GlobalEscape))
                //TODO
                case Invokedynamic(_, _, _, _, _) ⇒
                    throw new RuntimeException("Requires Invokedynamic resolution (see java8LambdaExpressions...")

                // TODO on standard javac this won't happen
                case Checkcast(_, _, _) ⇒
                    throw new RuntimeException("Not yet implemented")
                case _ ⇒ None
            }
            case _ ⇒ None
        }

        (p2s, result)
    }

    def determineEscape(e: AllocationSite): PropertyComputationResult = {
        val method = e.method
        val TACode(code, _, _, _) = project.get(DefaultTACAIKey)(method)
        var p2s = IntSet.empty
        for (stmt ← code) {
            determineEscapeStmt(stmt, e, p2s) match {
                case (_, Some(result)) ⇒ return result
                case (np2s, None)      ⇒ p2s = np2s
            }
        }

        Result(method, NoEscape)
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

