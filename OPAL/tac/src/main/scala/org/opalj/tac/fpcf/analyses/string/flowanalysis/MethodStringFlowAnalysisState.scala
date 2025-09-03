/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string
package flowanalysis

import scala.collection.mutable

import org.opalj.ai.ImmediateVMExceptionsOriginOffset
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.string.StringTreeInvalidElement
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeParameter
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.EOptionP
import org.opalj.si.flowanalysis.DataFlowAnalysis
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * The state of the [[MethodStringFlowAnalysis]] containing data flow analysis relevant information such as flow graphs
 * and all string flow dependees for the method under analysis.
 *
 * @see [[MethodStringFlowAnalysis]]
 *
 * @author Maximilian Rüsch
 */
case class MethodStringFlowAnalysisState(
    entity:       Method,
    dm:           DefinedMethod,
    tac:          TAC,
    flowAnalysis: DataFlowAnalysis[StringTreeNode, StringTreeEnvironment]
) {

    private val pcToDependeeMapping: mutable.Map[Int, EOptionP[MethodPC, StringFlowFunctionProperty]] =
        mutable.Map.empty
    private val pcToWebChangeMapping: mutable.Map[Int, Boolean] = mutable.Map.empty

    def updateDependee(pc: Int, dependee: EOptionP[MethodPC, StringFlowFunctionProperty]): Unit = {
        val prevOpt = pcToDependeeMapping.get(pc)
        if (prevOpt.isEmpty || prevOpt.get.hasNoUBP || (dependee.hasUBP && prevOpt.get.ub.webs != dependee.ub.webs)) {
            pcToWebChangeMapping.update(pc, true)
        }
        pcToDependeeMapping.update(pc, dependee)
    }

    def dependees: Set[EOptionP[MethodPC, StringFlowFunctionProperty]] =
        pcToDependeeMapping.values.filter(_.isRefinable).toSet

    def hasDependees: Boolean = pcToDependeeMapping.valuesIterator.exists(_.isRefinable)

    def getFlowFunctionsByPC: Map[Int, StringFlowFunction] = pcToDependeeMapping.toMap.transform { (_, eOptP) =>
        if (eOptP.hasUBP) eOptP.ub.flow
        else StringFlowFunctionProperty.ub.flow
    }

    /**
     * Collects def-use-webs from all dependees and for the parameters.
     */
    private def webs(): IndexedSeq[PDUWeb] = {
        pcToDependeeMapping.values.flatMap { v =>
            if (v.hasUBP) v.ub.webs
            else StringFlowFunctionProperty.ub.webs
        }.toIndexedSeq.appendedAll(tac.params.parameters.zipWithIndex.map {
            case (param, index) =>
                PDUWeb(IntTrieSet(-index - 1), if (param != null) param.useSites else IntTrieSet.empty)
        })
    }

    private def mergeWebs(webs: IndexedSeq[PDUWeb]): Array[PDUWeb] = {
        val indexedWebs = mutable.ArrayBuffer.empty[PDUWeb]
        val defPCToWebIndex = mutable.Map.empty[Int, Int]
        // Combine all webs that share common definitions (defPC)
        webs.foreach { web =>
            val existingDefPCs = web.defPCs.filter(defPCToWebIndex.contains)
            if (existingDefPCs.nonEmpty) { // At least one already processed web with a common defPC exist
                val indices = existingDefPCs.iterator.map(defPCToWebIndex).distinct.toSeq
                if (indices.size == 1) { // Exactly one already processed web has a common defPC, merge with that
                    val index = indices.head
                    indexedWebs.update(index, indexedWebs(index).combine(web))
                    web.defPCs.foreach(defPCToWebIndex.update(_, index))
                } else { // More than one already processed web has a common defPC, merge all of them
                    val newIndex = indices.head
                    val originalWebs = indices.map(indexedWebs)
                    indexedWebs.update(newIndex, originalWebs.reduce(_.combine(_)).combine(web))
                    indices.tail.foreach(indexedWebs.update(_, null))
                    originalWebs.foreach(_.defPCs.foreach(defPCToWebIndex.update(_, newIndex)))
                    web.defPCs.foreach(defPCToWebIndex.update(_, newIndex))
                }
            } else { // No already processed web with a common defPC exists, store this web as a distinct one
                val newIndex = indexedWebs.length
                indexedWebs.append(web)
                web.defPCs.foreach(defPCToWebIndex.update(_, newIndex))
            }
        }
        indexedWebs.toArray
    }

    private var _startEnv: StringTreeEnvironment = StringTreeEnvironment(Map.empty)

    /**
     * Computes the starting environment for the data-flow analysis
     */
    def getStartEnvAndReset(implicit highSoundness: Boolean): StringTreeEnvironment = {
        // Only compute a new starting environment if dependees have changed
        if (pcToWebChangeMapping.exists(_._2)) {
            val allWebs = webs()
            val mergedWebs = mergeWebs(allWebs)

            val startMap = mergedWebs.filter(_ != null)
                .map { (web: PDUWeb) =>
                    val defPC = web.defPCs.toList.min

                    if (defPC >= 0) {
                        (web, StringTreeInvalidElement) // local variables start as Invalid
                    } else if (defPC < -1 && defPC > ImmediateVMExceptionsOriginOffset) {
                        (web, StringTreeParameter.forParameterPC(defPC)) // parameters start as placeholders
                    } else {
                        // IMPROVE interpret "this" (parameter pc -1) with reference to String and StringBuilder classes
                        (web, StringInterpreter.failureTree) // "this" starts as failure since it can't be handled yet
                    }
                }.toMap

            _startEnv = StringTreeEnvironment(startMap)
            pcToWebChangeMapping.mapValuesInPlace((_, _) => false)
        }
        _startEnv
    }
}
