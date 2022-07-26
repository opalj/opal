/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac

import java.net.URL

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger.info
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.FinalP
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaAbnormalReturn
import org.opalj.br.fpcf.properties.EscapeViaParameter
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.analyses.escape.EagerSimpleEscapeAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer

/**
 * An evaluation of the impact of field/array writes to the
 * [[org.opalj.tac.fpcf.analyses.escape.SimpleEscapeAnalysis]] as the three-address code currently
 * does not support may-alias information for fields and arrays.
 *
 * Among others, it estimates the number of arraystores/putfields with non-escaping objects as
 * receiver.
 *
 * @author Florian Kuebler
 */
object FieldAndArrayUsageAnalysis extends ProjectAnalysisApplication {

    override def title: String = "Field and array usage evaluation"

    override def description: String = {
        "Evaluates the impact of field and array usage to the escape analysis."
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
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

        val (defSites, ass) = time {
            val defSites = project.get(DefinitionSitesKey)
            (defSites, defSites.getAllocationSites)
        } { t => info("progress", s"allocationSites took ${t.toSeconds}") }

        val propertyStore = time {
            project.get(PropertyStoreKey)
        } { t => info("progress", s"initialization of property store took ${t.toSeconds}") }
        time {
            val manager = project.get(FPCFAnalysesManagerKey)
            manager.runAll(
                EagerSimpleEscapeAnalysis,
                LazyL0BaseAIAnalysis,
                TACAITransformer /* LazyL0TACAIAnalysis */ )
        } { t => info("progress", s"escape analysis took ${t.toSeconds}") }
        for {
            as <- ass
            pc = as.pc
            m = as.method
            body = m.body.get
            FinalP(tacai) = propertyStore(m, org.opalj.tac.fpcf.properties.TACAI.key)
            code = tacai.tac.get
            stmts = code.stmts
            index = stmts indexWhere { stmt => stmt.pc == pc }
            if index != -1
        } {
            allocations += 1
            stmts(index) match {
                case Assignment(`pc`, DVar(_, uses), New(`pc`, _) | NewArray(`pc`, _, _)) =>
                    nonDeadAllocations += 1
                    for (use <- uses) {
                        stmts(use) match {
                            case PutField(_, _, name, _, objRef, value) =>
                                if (value.isVar && value.asVar.definedBy.contains(index)) {
                                    putFields += 1
                                    if (objRef.isVar && objRef.asVar.definedBy != IntTrieSet(-1)) {
                                        val defSitesOfObjRef = objRef.asVar.definedBy
                                        if (defSitesOfObjRef.exists { defSite =>
                                            if (defSite > 0) {
                                                stmts(defSite) match {
                                                    case Assignment(_, _, New(_, _)) => true
                                                    case _                           => false
                                                }
                                            } else false
                                        }) {
                                            putFieldsOfAllocation += 1
                                            for (stmt <- stmts) {
                                                stmt match {
                                                    case Assignment(_, DVar(_, _), GetField(_, _, `name`, _, objRef2)) if objRef2.isVar =>
                                                        if (objRef2.asVar.definedBy.exists(defSitesOfObjRef.contains)) {
                                                            getFields += 1
                                                        }

                                                    case _ =>
                                                }
                                            }
                                        }
                                    }
                                }
                            case arrayStore @ ArrayStore(pcOfStore, arrayRef, _, value) =>
                                if (value.isVar && value.asVar.definedBy.contains(index)) {
                                    arrayStores += 1

                                    if (arrayRef.isVar) {
                                        val defSitesOfArray = arrayRef.asVar.definedBy

                                        // nesting filter and map as collect is not available
                                        val pcsOfNewArrays = defSitesOfArray withFilter { defSite =>
                                            defSite > 0 && (stmts(defSite) match {
                                                case Assignment(_, _, NewArray(_, _, _)) => true
                                                case _                                   => false
                                            })
                                        } map {
                                            stmts(_) match {
                                                case Assignment(pc, _, _) => pc
                                                case _                    => throw new RuntimeException()
                                            }
                                        }

                                        if (pcsOfNewArrays.nonEmpty) {
                                            arrayStoresOfAllocation += 1
                                            for (pc <- pcsOfNewArrays) {
                                                val as = defSites(m, pc)
                                                propertyStore(as, EscapeProperty.key) match {
                                                    case FinalP(NoEscape | AtMost(NoEscape)) =>
                                                        maybeNoEscapingArrays += 1
                                                    case FinalP(EscapeInCallee | AtMost(EscapeInCallee)) =>
                                                        maybeInCalleeArrays += 1
                                                    case FinalP(EscapeViaParameter | AtMost(EscapeViaParameter)) =>
                                                        maybeViaParamArrays += 1
                                                    case FinalP(EscapeViaReturn | AtMost(EscapeViaReturn)) =>
                                                        maybeViaReturn += 1
                                                    case FinalP(EscapeViaAbnormalReturn | AtMost(EscapeViaAbnormalReturn)) =>
                                                        maybeViaAbnormal += 1
                                                    case FinalP(p) if p.isBottom => globalArrays += 1
                                                    case _                       => maybeInCallerArrays += 1
                                                }
                                            }

                                            for {
                                                Assignment(_, DVar(_, _), ArrayLoad(_, _, arrayRef2)) <- stmts
                                                if arrayRef2.isVar
                                                if arrayRef2.asVar.definedBy.exists(defSitesOfArray.contains)
                                            } {
                                                arrayLoads += 1
                                                println(
                                                    s"""
                                                       |${m.toJava}:
                                                       |  ${body.lineNumber(as.pc).map("line: "+_.toString).getOrElse("")} new $as
                                                       |  ${body.lineNumber(pcOfStore).map("line: "+_.toString).getOrElse("")} $arrayStore}
                                                       |""".stripMargin
                                                )
                                            }
                                        }
                                    }
                                }
                            case _ =>
                        }
                    }
                case _ =>
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
