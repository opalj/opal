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
package org.opalj.tac

import org.opalj.br.analyses.Project
import java.net.URL

import org.opalj.ai.common.DefinitionSitesKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.BasicReport
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.analyses.escape.EagerSimpleEscapeAnalysis
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.EscapeViaParameter
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.log.LogContext
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.log.OPALLogger.info

/**
 *
 * @author Florian Kübler
 */
object FieldAndArrayUsageAnalysis extends DefaultOneStepAnalysis {

    override def title: String = ""

    override def description: String = {
        ""
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        var putFields = 0
        var putFieldsOfAllocation = 0
        var getFields = 0
        var arrayStores = 0
        var arrayStoresOfAllocation = 0
        var arrayLoads = 0
        var maybeNoEscapingArrays = 0
        var maybeInCalleeArrays = 0
        var maybeInCallerArrays = 0
        var maybeViaParamArrays = 0
        var maybeViaReturn = 0
        var maybeViaAbnormal = 0
        var globalArrays = 0
        var allocations = 0
        var nonDeadAllocations = 0

        implicit val logContext: LogContext = project.logContext

        val tacaiProvider = time {
            val tacai = project.get(DefaultTACAIKey)
            tacai
        } { t ⇒ info("progress", s"generating 3-address code took ${t.toSeconds}") }

        val (defSites, ass) = time {
            val defSites = project.get(DefinitionSitesKey)
            (defSites, defSites.getAllocationSites)
        } { t ⇒ info("progress", s"allocationSites took ${t.toSeconds}") }

        val propertyStore = time {
            project.get(PropertyStoreKey)
        } { t ⇒ info("progress", s"initialization of property store took ${t.toSeconds}") }
        time {
            EagerSimpleEscapeAnalysis.start(project, propertyStore)
            propertyStore.waitOnPhaseCompletion()
        } { t ⇒ info("progress", s"escape analysis took ${t.toSeconds}") }
        for {
            as ← ass
            pc = as.pc
            m = as.method
            code = tacaiProvider(m).stmts
            lineNumbers = tacaiProvider(m).lineNumberTable
            index = code indexWhere { stmt ⇒ stmt.pc == pc }
            if index != -1
        } {
            allocations += 1
            code(index) match {
                case Assignment(`pc`, DVar(_, uses), New(`pc`, _) | NewArray(`pc`, _, _)) ⇒
                    nonDeadAllocations += 1
                    for (use ← uses) {
                        code(use) match {
                            case PutField(_, _, name, _, objRef, value) ⇒
                                if (value.isVar && value.asVar.definedBy.contains(index)) {
                                    putFields += 1
                                    if (objRef.isVar && objRef.asVar.definedBy != IntTrieSet(-1)) {
                                        val defSitesOfObjRef = objRef.asVar.definedBy
                                        if (defSitesOfObjRef.exists { defSite ⇒
                                            if (defSite > 0) {
                                                code(defSite) match {
                                                    case Assignment(_, _, New(_, _)) ⇒ true
                                                    case _                           ⇒ false
                                                }
                                            } else false
                                        }) {
                                            putFieldsOfAllocation += 1
                                            for (stmt ← code) {
                                                stmt match {
                                                    case Assignment(_, DVar(_, _), GetField(_, _, `name`, _, objRef2)) if objRef2.isVar ⇒
                                                        if (objRef2.asVar.definedBy.exists(defSitesOfObjRef.contains)) {
                                                            getFields += 1
                                                            //println(s"${m.toJava} ${if (lineNumbers.nonEmpty) lineNumbers.get.lookupLineNumber(as.pc)} ${as.allocatedType}")
                                                        }

                                                    case _ ⇒
                                                }
                                            }
                                        }
                                    }
                                }
                            case arrayStore @ ArrayStore(pcOfStore, arrayRef, _, value) ⇒
                                if (value.isVar && value.asVar.definedBy.contains(index)) {
                                    arrayStores += 1

                                    if (arrayRef.isVar) {
                                        val defSitesOfArray = arrayRef.asVar.definedBy

                                        // nesting filter and map as collect is not available
                                        val pcsOfNewArrays = defSitesOfArray withFilter { defSite ⇒
                                            defSite > 0 && (code(defSite) match {
                                                case Assignment(_, _, NewArray(_, _, _)) ⇒ true
                                                case _                                   ⇒ false
                                            })
                                        } map {
                                            code(_) match {
                                                case Assignment(pc, _, _) ⇒ pc
                                                case _                    ⇒ throw new RuntimeException()
                                            }
                                        }

                                        if (pcsOfNewArrays.nonEmpty) {
                                            arrayStoresOfAllocation += 1
                                            for (pc ← pcsOfNewArrays) {
                                                val as = defSites(m, pc)
                                                propertyStore(as, EscapeProperty.key) match {
                                                    case FinalEP(_, NoEscape | AtMost(NoEscape)) ⇒
                                                        maybeNoEscapingArrays += 1
                                                    case FinalEP(_, EscapeInCallee | AtMost(EscapeInCallee)) ⇒
                                                        maybeInCalleeArrays += 1
                                                    case FinalEP(_, EscapeViaParameter | AtMost(EscapeViaParameter)) ⇒
                                                        maybeViaParamArrays += 1
                                                    case FinalEP(_, EscapeViaReturn | AtMost(EscapeViaReturn)) ⇒
                                                        maybeViaReturn += 1
                                                    case FinalEP(_, EscapeViaAbnormalReturn | AtMost(EscapeViaAbnormalReturn)) ⇒
                                                        maybeViaAbnormal += 1
                                                    case FinalEP(_, p) if p.isBottom ⇒ globalArrays += 1
                                                    case _                           ⇒ maybeInCallerArrays += 1
                                                }
                                            }

                                            for (stmt ← code) {
                                                stmt match {
                                                    case Assignment(_, DVar(_, _), ArrayLoad(_, _, arrayRef2)) if arrayRef2.isVar ⇒
                                                        if (arrayRef2.asVar.definedBy.exists(defSitesOfArray.contains)) {
                                                            arrayLoads += 1
                                                            println(
                                                                s"""
                                                                    |${m.toJava}:
                                                                    |  ${if (lineNumbers.nonEmpty) lineNumbers.get.lookupLineNumber(as.pc)} new ${as}
                                                                    |  ${if (lineNumbers.nonEmpty) lineNumbers.get.lookupLineNumber(pcOfStore)} $arrayStore}
                                                                 """.stripMargin
                                                            )
                                                        }
                                                    case _ ⇒
                                                }
                                            }
                                        }
                                    }
                                }
                            case _ ⇒
                        }
                    }
                case _ ⇒
            }
        }

        val message =
            s"""
               |# of allocations $allocations
               |# of non dead allocations: $nonDeadAllocations
               |# of putfields: $putFields
               |# of putfields on new fields $putFieldsOfAllocation
               |# of getfields after puts: $getFields
               |# of arraystores: $arrayStores
               |# of arraystores on new array $arrayStoresOfAllocation
               |# of no escaping new arrays: $maybeNoEscapingArrays
               |# of escape in callee new arrays: $maybeInCalleeArrays
               |# of escape via param new arrays: $maybeViaParamArrays
               |# of escape via return new arrays: $maybeViaReturn
               |# of escape via abnormal new arrays: $maybeViaAbnormal
               |# of escape in caller new arrays: $maybeInCallerArrays
               |# of global new arrays: $globalArrays
               |# of arrayloads after store: $arrayLoads"""

        BasicReport(message.stripMargin('|'))
    }

}
