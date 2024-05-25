/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package tac
package fpcf
package analyses
package string

import scala.collection.mutable

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.fpcf.EOptionP
import org.opalj.tac.fpcf.analyses.string.flowanalysis.ControlTree
import org.opalj.tac.fpcf.analyses.string.flowanalysis.FlowGraph
import org.opalj.tac.fpcf.analyses.string.flowanalysis.SuperFlowGraph
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.MethodStringFlow
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringFlowFunctionProperty

/**
 * This class is to be used to store state information that are required at a later point in
 * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
 * have all required information ready for a final result.
 */
case class StringAnalysisState(entity: SContext, var stringFlowDependee: EOptionP[Method, MethodStringFlow]) {

    def hasDependees: Boolean = stringFlowDependee.isRefinable
    def dependees: Set[EOptionP[Method, MethodStringFlow]] = Set(stringFlowDependee)
}

/**
 * This class is to be used to store state information that are required at a later point in
 * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
 * have all required information ready for a final result.
 */
case class ComputationState(entity: Method, dm: DefinedMethod, var tacDependee: EOptionP[Method, TACAI]) {

    def tac: TAC = {
        if (tacDependee.hasUBP && tacDependee.ub.tac.isDefined)
            tacDependee.ub.tac.get
        else
            throw new IllegalStateException("Cannot get a TAC from a TACAI with no or empty upper bound!")
    }

    var flowGraph: FlowGraph = _
    var superFlowGraph: SuperFlowGraph = _
    var controlTree: ControlTree = _

    private val pcToDependeeMapping: mutable.Map[Int, EOptionP[MethodPC, StringFlowFunctionProperty]] =
        mutable.Map.empty

    def updateDependee(pc: Int, dependee: EOptionP[MethodPC, StringFlowFunctionProperty]): Unit =
        pcToDependeeMapping.update(pc, dependee)

    def dependees: Set[EOptionP[MethodPC, StringFlowFunctionProperty]] =
        pcToDependeeMapping.values.filter(_.isRefinable).toSet

    def hasDependees: Boolean = pcToDependeeMapping.valuesIterator.exists(_.isRefinable)

    def getFlowFunctionsByPC: Map[Int, StringFlowFunction] = pcToDependeeMapping.map { kv =>
        (
            kv._1,
            if (kv._2.hasUBP) kv._2.ub.flow
            else StringFlowFunctionProperty.ub.flow
        )
    }.toMap

    def getWebs: Iterator[PDUWeb] = pcToDependeeMapping.values.flatMap { v =>
        if (v.hasUBP) v.ub.webs
        else StringFlowFunctionProperty.ub.webs
    }.toSeq.sortBy(_.defPCs.toList.min).foldLeft(Seq.empty[PDUWeb]) { (reducedWebs, web) =>
        val index = reducedWebs.indexWhere(_.identifiesSameVarAs(web))
        if (index == -1)
            reducedWebs :+ web
        else
            reducedWebs.updated(index, reducedWebs(index).combine(web))
    }.iterator
}

case class InterpretationState(pc: Int, dm: DefinedMethod, var tacDependee: EOptionP[Method, TACAI]) {

    def tac: TAC = {
        if (tacDependee.hasUBP && tacDependee.ub.tac.isDefined)
            tacDependee.ub.tac.get
        else
            throw new IllegalStateException("Cannot get a TAC from a TACAI with no or empty upper bound!")
    }
}
