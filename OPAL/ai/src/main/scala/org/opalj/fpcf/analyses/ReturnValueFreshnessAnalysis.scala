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

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.Method
import org.opalj.br.AllocationSite
import org.opalj.br.BaseType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.AllocationSites
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.Conditional
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.ConditionalFreshReturnValue
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.DUVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.TACode
import org.opalj.tac.ReturnValue
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.Assignment
import org.opalj.tac.Const

/**
 * An analysis that determines for a given method, whether its the return value is a fresh object,
 * that is created within the method and does not escape by other than [[EscapeViaReturn]].
 *
 * In other words, it aggregates the escape information for all allocation-sites, that might be used
 * as return value.
 *
 * @author Florian Kübler
 */
class ReturnValueFreshnessAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {
    private[this] val tacaiProvider: (Method) ⇒ TACode[TACMethodParameter, DUVar[(Domain with RecordDefUse)#DomainValue]] = project.get(DefaultTACAIKey)
    private[this] val allocationSites: AllocationSites = propertyStore.context[AllocationSites]

    /**
     * Determines the freshness of the return value.
     */
    def determineFreshness(m: Method): PropertyComputationResult = {
        // base types are always fresh
        if (m.returnType.isInstanceOf[BaseType]) {
            Result(m, PrimitiveReturnValue)
        } else {
            var dependees: Set[EOptionP[AllocationSite, EscapeProperty]] = Set.empty
            val code = tacaiProvider(m).stmts

            // for every return-value statement check the def-sites
            for {
                ReturnValue(_, expr) ← code
                defSite ← expr.asVar.definedBy
            } {
                if (defSite >= 0) {
                    val stmt = code(defSite)
                    stmt match {
                        // if the def-site of the return-value statement is a new, we check the escape state
                        case Assignment(pc, _, New(_, _) | NewArray(_, _, _)) ⇒
                            val allocationSite = allocationSites(m)(pc)
                            val resultState = propertyStore(allocationSite, EscapeProperty.key)
                            resultState match {
                                case EP(_, EscapeViaReturn)              ⇒
                                case EP(_, NoEscape | EscapeInCallee)    ⇒ throw new RuntimeException("unexpected result")
                                case EP(_, p) if p.isFinal               ⇒ return Result(m, NoFreshReturnValue)
                                case EP(_, AtMost(_))                    ⇒ return Result(m, NoFreshReturnValue)
                                case EP(_, Conditional(NoEscape))        ⇒ throw new RuntimeException("unexpected result")
                                case EP(_, Conditional(EscapeInCallee))  ⇒ throw new RuntimeException("unexpected result")
                                case EP(_, Conditional(EscapeViaReturn)) ⇒ dependees += resultState
                                case _                                   ⇒ dependees += resultState
                            }
                        // const values are handled as fresh
                        case Assignment(_, _, _: Const) ⇒

                        // other kinds of assignments came from other methods, fields etc, which we do not track
                        case Assignment(_, _, _)        ⇒ return Result(m, NoFreshReturnValue)
                        case _                          ⇒ throw new RuntimeException("not yet implemented")
                    }
                }

            }

            /**
             * A continuation function, that handles updates for the escape state.
             */
            def c(e: Entity, p: Property, ut: UpdateType): PropertyComputationResult = {
                p match {

                    case NoEscape | EscapeInCallee ⇒
                        throw new RuntimeException("unexpected result")
                    case EscapeViaReturn ⇒
                        dependees = dependees.filter { _.e ne e }
                        if (dependees.isEmpty) {
                            Result(m, FreshReturnValue)
                        } else {
                            IntermediateResult(m, ConditionalFreshReturnValue, dependees, c)
                        }
                    case _: EscapeProperty if p.isFinal ⇒ Result(m, NoFreshReturnValue)
                    case AtMost(_)                      ⇒ Result(m, NoFreshReturnValue)

                    case p @ Conditional(EscapeViaReturn) ⇒
                        val newEP = EP(e.asInstanceOf[AllocationSite], p)
                        dependees = dependees.filter(_.e ne e) + newEP
                        IntermediateResult(m, ConditionalFreshReturnValue, dependees, c)

                    case Conditional(NoEscape) | Conditional(EscapeInCallee) ⇒
                        throw new RuntimeException("unexpected result")

                }
            }

            if (dependees.isEmpty) {
                Result(m, FreshReturnValue)
            } else {
                IntermediateResult(m, ConditionalFreshReturnValue, dependees, c)
            }
        }
    }
}

object ReturnValueFreshnessAnalysis extends FPCFEagerAnalysisScheduler {

    override def derivedProperties: Set[PropertyKind] = Set(ReturnValueFreshness)

    override def usedProperties: Set[PropertyKind] = Set(EscapeProperty)

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new ReturnValueFreshnessAnalysis(project)
        propertyStore.scheduleForEntities(project.allMethodsWithBody)(analysis.determineFreshness)
        analysis
    }
}
