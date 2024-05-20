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
import org.opalj.tac.fpcf.properties.string.StringFlowFunction
import org.opalj.tac.fpcf.properties.string.StringTreeEnvironment

/**
 * This class is to be used to store state information that are required at a later point in
 * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
 * have all required information ready for a final result.
 */
case class ComputationState(dm: DefinedMethod, entity: SContext, var tacDependee: EOptionP[Method, TACAI]) {

    def tac: TAC = {
        if (tacDependee.hasUBP && tacDependee.ub.tac.isDefined)
            tacDependee.ub.tac.get
        else
            throw new IllegalStateException("Cannot get a TAC from a TACAI with no or empty upper bound!")
    }

    var startEnv: StringTreeEnvironment = _
    var flowGraph: FlowGraph = _
    var superFlowGraph: SuperFlowGraph = _
    var controlTree: ControlTree = _

    private val pcToDependeeMapping: mutable.Map[Int, EOptionP[MethodPC, StringFlowFunction]] = mutable.Map.empty

    def updateDependee(pc: Int, dependee: EOptionP[MethodPC, StringFlowFunction]): Unit =
        pcToDependeeMapping.update(pc, dependee)

    def dependees: Set[EOptionP[MethodPC, StringFlowFunction]] = pcToDependeeMapping.values.filter(_.isRefinable).toSet

    def hasDependees: Boolean = pcToDependeeMapping.valuesIterator.exists(_.isRefinable)

    def getFlowFunctionsByPC: Map[Int, StringFlowFunction] = pcToDependeeMapping.map { kv =>
        (
            kv._1,
            if (kv._2.hasUBP) kv._2.ub
            else StringFlowFunction.ub
        )
    }.toMap
}

case class InterpretationState(pc: Int, dm: DefinedMethod, var tacDependee: EOptionP[Method, TACAI]) {

    def tac: TAC = {
        if (tacDependee.hasUBP && tacDependee.ub.tac.isDefined)
            tacDependee.ub.tac.get
        else
            throw new IllegalStateException("Cannot get a TAC from a TACAI with no or empty upper bound!")
    }
}
