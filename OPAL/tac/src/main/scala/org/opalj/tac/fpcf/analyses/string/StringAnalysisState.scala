/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import scala.collection.mutable

import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeOr
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyStore
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.MethodStringFlow

private[string] case class ContextFreeStringAnalysisState(
    entity:                 VariableDefinition,
    var stringFlowDependee: EOptionP[Method, MethodStringFlow],
    var hitMaximumDepth:    Boolean = false
) {

    def hasDependees: Boolean = stringFlowDependee.isRefinable

    def dependees: Set[EOptionP[Entity, Property]] = Set(stringFlowDependee)
}

private[string] class ContextStringAnalysisState private (
    _entity:                       VariableContext,
    var _stringDependee:           EOptionP[VariableDefinition, StringConstancyProperty],
    private var _parameterIndices: Set[Int]
) {

    def entity: VariableContext = _entity
    def dm: DeclaredMethod = _entity.context.method

    def updateStringDependee(stringDependee: EPS[VariableDefinition, StringConstancyProperty]): Unit = {
        _stringDependee = stringDependee
        _parameterIndices ++= stringDependee.ub.tree.parameterIndices
    }
    private def stringTree: StringTreeNode = _stringDependee.ub.tree
    def parameterIndices: Set[Int] = _parameterIndices

    // Parameter StringConstancy
    private val _paramDependees: mutable.Map[MethodParameterContext, EOptionP[
        MethodParameterContext,
        StringConstancyProperty
    ]] =
        mutable.Map.empty

    def registerParameterDependee(dependee: EOptionP[MethodParameterContext, StringConstancyProperty]): Unit = {
        if (_paramDependees.contains(dependee.e)) {
            throw new IllegalArgumentException(s"Tried to register the same parameter dependee twice: $dependee")
        }

        updateParamDependee(dependee)
    }
    def updateParamDependee(dependee: EOptionP[MethodParameterContext, StringConstancyProperty]): Unit =
        _paramDependees(dependee.e) = dependee

    def currentTreeUB: StringTreeNode = {
        if (_stringDependee.hasUBP) {
            val paramTrees = _paramDependees.map { kv =>
                (
                    kv._1.index,
                    if (kv._2.hasUBP) kv._2.ub.tree
                    else StringTreeNode.ub
                )
            }.toMap

            stringTree.replaceParameters(paramTrees).simplify
        } else {
            StringTreeNode.ub
        }
    }

    def hasDependees: Boolean = _stringDependee.isRefinable || _paramDependees.valuesIterator.exists(_.isRefinable)

    def dependees: Set[EOptionP[Entity, Property]] = {
        val preliminaryDependees =
            _paramDependees.valuesIterator.filter(_.isRefinable) ++
                Some(_stringDependee).filter(_.isRefinable)

        preliminaryDependees.toSet
    }
}

object ContextStringAnalysisState {

    def apply(
        entity:         VariableContext,
        stringDependee: EOptionP[VariableDefinition, StringConstancyProperty]
    ): ContextStringAnalysisState = {
        val parameterIndices = if (stringDependee.hasUBP) stringDependee.ub.tree.parameterIndices
        else Set.empty[Int]
        new ContextStringAnalysisState(entity, stringDependee, parameterIndices)
    }
}

private[string] case class MethodParameterContextStringAnalysisState(
    private val _entity: MethodParameterContext
) {

    def entity: MethodParameterContext = _entity
    def dm: DeclaredMethod = _entity.context.method
    def index: Int = _entity.index

    // Callers
    private[string] type CallerContext = (Context, Int)
    var _callersDependee: Option[EOptionP[DeclaredMethod, Callers]] = None
    private val _callerContexts: mutable.Map[CallerContext, Option[Call[V]]] = mutable.Map.empty
    private val _callerContextsByMethod: mutable.Map[Method, Seq[CallerContext]] = mutable.Map.empty

    def updateCallers(newCallers: EOptionP[DeclaredMethod, Callers]): Unit = _callersDependee = Some(newCallers)

    def addCallerContext(callerContext: CallerContext): Unit = {
        _callerContexts.update(callerContext, None)
        _callerContextsByMethod.updateWith(callerContext._1.method.definedMethod) {
            case None       => Some(Seq(callerContext))
            case Some(prev) => Some(callerContext +: prev)
        }
    }
    def getCallerContexts(m: Method): Seq[CallerContext] = _callerContextsByMethod(m)
    def addCallExprInformationForContext(callerContext: CallerContext, expr: Call[V]): Option[Call[V]] =
        _callerContexts.put(callerContext, Some(expr)).flatten

    // TACAI
    private val _tacaiDependees: mutable.Map[Method, EOptionP[Method, TACAI]] = mutable.Map.empty
    var _discoveredUnknownTAC: Boolean = false

    def updateTacaiDependee(tacEOptP: EOptionP[Method, TACAI]): Unit = _tacaiDependees(tacEOptP.e) = tacEOptP
    def getTacaiForContext(callerContext: CallerContext)(implicit ps: PropertyStore): EOptionP[Method, TACAI] = {
        val m = callerContext._1.method.definedMethod

        if (_tacaiDependees.contains(m)) {
            _tacaiDependees(m)
        } else {
            val tacEOptP = ps(m, TACAI.key)
            _tacaiDependees(tacEOptP.e) = tacEOptP
            tacEOptP
        }
    }
    def getTACForContext(callerContext: CallerContext)(implicit ps: PropertyStore): TAC =
        getTacaiForContext(callerContext).ub.tac.get

    // Parameter StringConstancy
    private val _methodToEntityMapping: mutable.Map[DefinedMethod, Seq[VariableContext]] = mutable.Map.empty
    private val _paramDependees: mutable.Map[VariableContext, EOptionP[VariableContext, StringConstancyProperty]] =
        mutable.Map.empty

    def registerParameterDependee(
        dm:       DefinedMethod,
        dependee: EOptionP[VariableContext, StringConstancyProperty]
    ): Unit = {
        if (_paramDependees.contains(dependee.e)) {
            throw new IllegalArgumentException(s"Tried to register the same parameter dependee twice: $dependee")
        }

        _methodToEntityMapping.updateWith(dm) {
            case None           => Some(Seq(dependee.e))
            case Some(previous) => Some((previous :+ dependee.e).sortBy(_.pc))
        }

        updateParamDependee(dependee)
    }
    def updateParamDependee(dependee: EOptionP[VariableContext, StringConstancyProperty]): Unit =
        _paramDependees(dependee.e) = dependee

    def currentTreeUB: StringTreeNode = {
        if (_discoveredUnknownTAC) {
            StringTreeNode.lb
        } else {
            val paramOptions = _methodToEntityMapping.keys.toSeq
                .sortBy(_.id)
                .flatMap { dm =>
                    _methodToEntityMapping(dm)
                        .map(_paramDependees)
                        .filter(_.hasUBP)
                        .map(_.ub.tree)
                }

            StringTreeOr(paramOptions).simplify
        }
    }

    def finalTree: StringTreeNode = {
        if (_methodToEntityMapping.isEmpty) {
            StringTreeNode.lb
        } else {
            currentTreeUB
        }
    }

    def hasDependees: Boolean = _callersDependee.exists(_.isRefinable) ||
        _tacaiDependees.valuesIterator.exists(_.isRefinable) ||
        _paramDependees.valuesIterator.exists(_.isRefinable)

    def dependees: Set[EOptionP[Entity, Property]] = {
        val preliminaryDependees =
            _tacaiDependees.valuesIterator.filter(_.isRefinable) ++
                _paramDependees.valuesIterator.filter(_.isRefinable) ++
                _callersDependee.filter(_.isRefinable)

        preliminaryDependees.toSet
    }
}
