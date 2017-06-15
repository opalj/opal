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

import org.opalj.collection.immutable.IntSet
import org.opalj.br.Method
import org.opalj.br.analyses.{SomeProject}
import org.opalj.fpcf.properties._
import org.opalj.tac._

case class EscapeEntity(method: Method, newExpr: New) extends Entity

class SimpleEscapeAnalysis private(final val project: SomeProject) extends FPCFAnalysis {

    val DEBUG = false;

    private def checkValue(value: Expr[_], p2s: IntSet) = {
        if (value.isInstanceOf[UVar[_]])
            value.asInstanceOf[UVar[_]].definedBy.exists(v ⇒ p2s.contains(v))
        else
            throw new RuntimeException("This should not happen")
    }

    private def checkParams(params: Seq[Expr[_]], p2s: IntSet) = {
        params.exists(value ⇒ checkValue(value, p2s))
    }

    def determineEscape(e: Method): PropertyComputationResult = {
        val method = e //e.method
        val tac = project.get(DefaultTACAIKey)
        val TACode(code, _, _, _) = tac(method)

        synchronized {
            if (DEBUG) println("----------------------------------------------")
            if (DEBUG) println("METHOD: " + method.name)
            var p2s = IntSet.empty
            for (stmt ← code) {
                if (DEBUG) println(stmt)
                stmt match {
                    case PutStatic(_, _, _, value) ⇒ if (checkValue(value, p2s)) {
                        if (DEBUG) println("ESCAPE")
                        return ImmediateResult(method, GlobalEscape)
                    }
                    case PutField(_, _, _, _, value) ⇒ if (checkValue(value, p2s)) {
                        if (DEBUG) println("ESCAPE")
                        return ImmediateResult(method, GlobalEscape)
                    }
                    case ReturnValue(_, value) ⇒ if (checkValue(value, p2s)) {
                        if (DEBUG) println("ESCAPE")
                        return ImmediateResult(method, GlobalEscape)
                    }
                    case ArrayStore(_, _, _, value) ⇒ if (checkValue(value, p2s)) {
                        if (DEBUG) println("ESCAPE")
                        return ImmediateResult(method, GlobalEscape)
                    }
                    case Throw(_, exception) ⇒ if (checkValue(exception, p2s)) {
                        if (DEBUG) println("ESCAPE")
                        return ImmediateResult(method, GlobalEscape)
                    }
                    case StaticMethodCall(_, _, _, _, _, params) ⇒
                        if (checkParams(params, p2s)) {
                            if (DEBUG) println("ESCAPE")
                            return ImmediateResult(method, GlobalEscape)
                        }
                    case VirtualMethodCall(_, _, _, _, _, receiver, params) ⇒
                        if (checkValue(receiver, p2s) || checkParams(params, p2s)) {
                            if (DEBUG) println("ESCAPE")
                            return ImmediateResult(method, GlobalEscape)
                        }
                    // TODO: should the constructor base local escape?
                    case NonVirtualMethodCall(_, _, _, _, _, _, params) ⇒
                        if (checkParams(params, p2s)) {
                            if (DEBUG) println("ESCAPE")
                            return ImmediateResult(method, GlobalEscape)
                        }
                    case Assignment(_, left, right) ⇒ right match {
                        case New(_, _) ⇒
                            p2s = p2s + left.asInstanceOf[DVar[_]].definedBy
                        case NonVirtualFunctionCall(_, _, _, _, _, _, params) ⇒
                            if (params.exists(value ⇒ value.asInstanceOf[UVar[_]].definedBy.exists(v ⇒ p2s.contains(v)))) {
                                if (DEBUG) println("ESCAPE")
                                return ImmediateResult(method, GlobalEscape)
                            }
                        case VirtualFunctionCall(_, _, _, _, _, receiver, params) ⇒
                            if (checkValue(receiver, p2s)
                                || params.exists(value ⇒ value.asInstanceOf[UVar[_]].definedBy.exists(v ⇒ p2s.contains(v)))) {
                                if (DEBUG) println("ESCAPE")
                                return ImmediateResult(method, GlobalEscape)
                            }
                        case StaticFunctionCall(_, _, _, _, _, params) ⇒
                            if (params.exists(value ⇒ value.asInstanceOf[UVar[_]].definedBy.exists(v ⇒ p2s.contains(v)))) {
                                if (DEBUG) println("ESCAPE")
                                return ImmediateResult(method, GlobalEscape)
                            }
                        case Invokedynamic(_, _, _, _, params) ⇒
                            if (params.exists(value ⇒ value.asInstanceOf[UVar[_]].definedBy.exists(v ⇒ p2s.contains(v)))) {
                                if (DEBUG) println("ESCAPE")
                                return ImmediateResult(method, GlobalEscape)
                            }
                        case Checkcast(_, value, cmpTpe) => {
                            if (DEBUG) println(value + " " + cmpTpe)
                        }
                        case _ ⇒
                    }
                    case _ ⇒
                }
                if (DEBUG) println(p2s)
                if (DEBUG) println()
            }

            if (DEBUG) println("----------------------------------------------")
        }
        Result(method, NoEscape)
    }
}

object SimpleEscapeAnalysis extends FPCFAnalysisRunner {

    def entitySelector(project: SomeProject): PartialFunction[Entity, Method] = {
        // TODO this is preperation to extract all allocation sites of a method
        case method: Method => {
            var result: List[EscapeEntity] = List()
            val TACode(code, _, _, _) = project.get(DefaultTACAIKey)(method)
            for (stmt <- code) {
                stmt match {
                    case Assignment(_, _, New(pc, tpe)) => result = EscapeEntity(method, New(pc, tpe)) :: result
                    case _ =>
                }
            }
            //result
            method
        }
        case _: EscapeEntity => throw new RuntimeException()
    }

    override def derivedProperties: Set[PropertyKind] = Set(EscapeProperty)

    override def usedProperties: Set[PropertyKind] = Set.empty

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new SimpleEscapeAnalysis(project)
        propertyStore <||< (entitySelector(project), analysis.determineEscape)
        analysis
    }
}

