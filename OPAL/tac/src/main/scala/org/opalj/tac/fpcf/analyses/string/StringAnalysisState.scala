/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package string

import scala.collection.mutable

import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.string.StringConstancyInformation
import org.opalj.br.fpcf.properties.string.StringConstancyProperty
import org.opalj.br.fpcf.properties.string.StringTreeNode
import org.opalj.br.fpcf.properties.string.StringTreeOr
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Property
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.string.MethodStringFlow

private[string] case class ContextFreeStringAnalysisState(
    entity:                 VariableDefinition,
    var stringFlowDependee: EOptionP[Method, MethodStringFlow]
) {

    def hasDependees: Boolean = stringFlowDependee.isRefinable

    def dependees: Set[EOptionP[Entity, Property]] = Set(stringFlowDependee)
}

private[string] case class ContextStringAnalysisState(
    entity:              VariableContext,
    var _stringDependee: EOptionP[VariableDefinition, StringConstancyProperty]
) {

    def dm: DeclaredMethod = entity.context.method
    def stringTree: StringTreeNode = _stringDependee.ub.sci.tree

    // Callers
    var _callersDependee: Option[EOptionP[DeclaredMethod, Callers]] = None

    // TACAI
    private type TACAIDepender = (Context, Int)
    private val _tacaiDependees: mutable.Map[Method, EOptionP[Method, TACAI]] = mutable.Map.empty
    private val _tacaiDependers: mutable.Map[Method, TACAIDepender] = mutable.Map.empty
    var _discoveredUnknownTAC: Boolean = false

    def registerTacaiDepender(tacEOptP: EOptionP[Method, TACAI], depender: TACAIDepender): Unit = {
        _tacaiDependers(tacEOptP.e) = depender
        _tacaiDependees(tacEOptP.e) = tacEOptP
    }
    def getDepender(m:                Method): TACAIDepender = _tacaiDependers(m)
    def updateTacaiDependee(tacEOptP: EOptionP[Method, TACAI]): Unit = _tacaiDependees(tacEOptP.e) = tacEOptP

    // Parameter StringConstancy
    private val _entityToParamIndexMapping: mutable.Map[VariableContext, (Int, Method)] = mutable.Map.empty
    private val _paramIndexToEntityMapping: mutable.Map[Int, mutable.Map[Method, Seq[VariableContext]]] =
        mutable.Map.empty.withDefaultValue(mutable.Map.empty)
    private val _paramDependees: mutable.Map[VariableContext, EOptionP[VariableContext, StringConstancyProperty]] =
        mutable.Map.empty
    private val _invalidParamReferences: mutable.Map[Method, Set[Int]] = mutable.Map.empty

    def registerParameterDependee(
        index:    Int,
        m:        Method,
        dependee: EOptionP[VariableContext, StringConstancyProperty]
    ): Unit = {
        if (!_paramIndexToEntityMapping.contains(index)) {
            _paramIndexToEntityMapping(index) = mutable.Map.empty
        }

        _paramIndexToEntityMapping(index).put(m, Seq(dependee.e))
        _entityToParamIndexMapping(dependee.e) = (index, m)
        _paramDependees(dependee.e) = dependee
    }
    def updateParamDependee(dependee: EOptionP[VariableContext, StringConstancyProperty]): Unit = {
        _paramDependees(dependee.e) = dependee
    }
    def registerInvalidParamReference(index: Int, m: Method): Unit = {
        _invalidParamReferences.updateWith(m)(ov => Some(ov.getOrElse(Set.empty) + index))
    }

    def currentSciUB: StringConstancyInformation = {
        if (_stringDependee.hasUBP) {
            val paramTrees = if (_discoveredUnknownTAC) {
                stringTree.collectParameterIndices.map((_, StringTreeNode.lb)).toMap
            } else {
                stringTree.collectParameterIndices.map { index =>
                    val paramOptions = _paramIndexToEntityMapping(index).keys.toSeq
                        .sortBy(_.fullyQualifiedSignature)
                        .flatMap { m =>
                            if (_invalidParamReferences.contains(m)) {
                                Some(StringTreeNode.ub)
                            } else {
                                _paramIndexToEntityMapping(index)(m)
                                    .sortBy(_.pc)
                                    .map(_paramDependees)
                                    .filter(_.hasUBP)
                                    .map(_.ub.sci.tree)
                            }
                        }

                    (index, StringTreeOr(paramOptions).simplify)
                }.toMap
            }

            StringConstancyInformation(stringTree.replaceParameters(paramTrees).simplify)
        } else {
            StringConstancyInformation.ub
        }
    }

    def finalSci: StringConstancyInformation = {
        if (_paramIndexToEntityMapping.valuesIterator.map(_.size).sum == 0) {
            val paramTrees = stringTree.collectParameterIndices.map((_, StringTreeNode.lb)).toMap
            StringConstancyInformation(stringTree.replaceParameters(paramTrees).simplify)
        } else {
            currentSciUB
        }
    }

    def hasDependees: Boolean = _stringDependee.isRefinable ||
        _callersDependee.exists(_.isRefinable) ||
        _tacaiDependees.valuesIterator.exists(_.isRefinable) ||
        _paramDependees.valuesIterator.exists(_.isRefinable)

    def dependees: Set[EOptionP[Entity, Property]] = {
        val preliminaryDependees =
            _tacaiDependees.valuesIterator.filter(_.isRefinable) ++
                _paramDependees.valuesIterator.filter(_.isRefinable) ++
                _callersDependee.filter(_.isRefinable)

        if (_stringDependee.isRefinable) {
            preliminaryDependees.toSet + _stringDependee
        } else {
            preliminaryDependees.toSet
        }
    }
}
