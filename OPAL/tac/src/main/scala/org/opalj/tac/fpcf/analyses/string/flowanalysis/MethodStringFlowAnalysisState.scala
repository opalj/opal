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
import org.opalj.br.fpcf.properties.string.StringTreeParameter
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.EOptionP
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * This class is to be used to store state information that are required at a later point in
 * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
 * have all required information ready for a final result.
 */
case class MethodStringFlowAnalysisState(entity: Method, dm: DefinedMethod, var tacDependee: EOptionP[Method, TACAI]) {

    def tac: TAC = {
        if (tacDependee.hasUBP && tacDependee.ub.tac.isDefined)
            tacDependee.ub.tac.get
        else
            throw new IllegalStateException("Cannot get a TAC from a TACAI with no or empty upper bound!")
    }

    var flowGraph: FlowGraph = _
    var superFlowGraph: SuperFlowGraph = _
    var controlTree: ControlTree = _
    var flowAnalysis: DataFlowAnalysis = _

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

    private def getWebs: IndexedSeq[PDUWeb] = {
        pcToDependeeMapping.values.flatMap { v =>
            if (v.hasUBP) v.ub.webs
            else StringFlowFunctionProperty.ub.webs
        }.toIndexedSeq.appendedAll(tac.params.parameters.zipWithIndex.map {
            case (param, index) =>
                PDUWeb(IntTrieSet(-index - 1), if (param != null) param.useSites else IntTrieSet.empty)
        })
    }

    private var _startEnv: StringTreeEnvironment = StringTreeEnvironment(Map.empty)
    def getStartEnvAndReset(implicit highSoundness: HighSoundness): StringTreeEnvironment = {
        if (pcToWebChangeMapping.exists(_._2)) {
            val webs = getWebs
            val indexedWebs = mutable.ArrayBuffer.empty[PDUWeb]
            val defPCToWebIndex = mutable.Map.empty[Int, Int]
            webs.foreach { web =>
                val existingDefPCs = web.defPCs.filter(defPCToWebIndex.contains)
                if (existingDefPCs.nonEmpty) {
                    val indices = existingDefPCs.toList.map(defPCToWebIndex).distinct
                    if (indices.size == 1) {
                        val index = indices.head
                        indexedWebs.update(index, indexedWebs(index).combine(web))
                        web.defPCs.foreach(defPCToWebIndex.update(_, index))
                    } else {
                        val newIndex = indices.head
                        val originalWebs = indices.map(indexedWebs)
                        indexedWebs.update(newIndex, originalWebs.reduce(_.combine(_)).combine(web))
                        indices.tail.foreach(indexedWebs.update(_, null))
                        originalWebs.foreach(_.defPCs.foreach(defPCToWebIndex.update(_, newIndex)))
                        web.defPCs.foreach(defPCToWebIndex.update(_, newIndex))
                    }
                } else {
                    val newIndex = indexedWebs.length
                    indexedWebs.append(web)
                    web.defPCs.foreach(defPCToWebIndex.update(_, newIndex))
                }
            }

            val startMap = indexedWebs.filter(_ != null)
                .map { web: PDUWeb =>
                    val defPC = web.defPCs.toList.min

                    if (defPC >= 0) {
                        (web, StringTreeInvalidElement)
                    } else if (defPC < -1 && defPC > ImmediateVMExceptionsOriginOffset) {
                        (web, StringTreeParameter.forParameterPC(defPC))
                    } else {
                        // IMPROVE interpret "this" (parameter pc -1) with reference to String and StringBuilder classes
                        (web, StringInterpreter.failureTree)
                    }
                }.toMap
            _startEnv = StringTreeEnvironment(startMap)
            pcToWebChangeMapping.mapValuesInPlace((_, _) => false)
        }
        _startEnv
    }
}
