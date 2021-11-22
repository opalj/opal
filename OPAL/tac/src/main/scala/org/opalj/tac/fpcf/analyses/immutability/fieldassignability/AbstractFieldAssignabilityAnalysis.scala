/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability
package fieldassignability

import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.PCs
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.FieldPrematurelyRead
import org.opalj.br.fpcf.properties.EffectivelyNonAssignable
import org.opalj.br.fpcf.properties.FieldAssignability
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.common.DefinitionSitesKey
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation
import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.DoubleType
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.ShortType
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.DeclaredMethod
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.cg.Callers

/**
 *
 * Encompasses the base functions for determining field reference immutability.
 *
 * @author Tobias Roth
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 */
trait AbstractFieldAssignabilityAnalysis extends FPCFAnalysis {

    type V = DUVar[ValueInformation]
    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)
    final val definitionSites = project.get(DefinitionSitesKey)
    implicit final val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    implicit final val typeProvider: TypeProvider = project.get(TypeProviderKey)

    case class State(
            field:                       Field,
            var fieldAssignability:      FieldAssignability                                       = EffectivelyNonAssignable,
            var prematurelyReadDependee: Option[EOptionP[Field, FieldPrematurelyRead]]            = None,
            var escapeDependees:         Set[EOptionP[(Context, DefinitionSite), EscapeProperty]] = Set.empty,
            var tacDependees:            Map[Method, EOptionP[Method, TACAI]]                     = Map.empty,
            var callerDependees:         Map[DeclaredMethod, EOptionP[DeclaredMethod, Callers]]   = Map.empty,
            var tacPCs:                  Map[Method, PCs]                                         = Map.empty
    ) {
        def hasDependees: Boolean =
            prematurelyReadDependee.isDefined || escapeDependees.nonEmpty ||
                tacDependees.valuesIterator.exists(_.isRefinable) ||
                callerDependees.valuesIterator.exists(_.isRefinable)

        def dependees: Set[EOptionP[Entity, Property]] =
            (prematurelyReadDependee ++ escapeDependees ++
                tacDependees.valuesIterator.filter(_.isRefinable) ++
                callerDependees.valuesIterator.filter(_.isRefinable)).toSet
    }

    /**
     * Returns TACode and Callers for a method if available, registering dependencies as necessary.
     */
    def getTACAIAndCallers(
        method: Method,
        pcs:    PCs
    )(implicit state: State): Option[(TACode[TACMethodParameter, V], Callers)] = {
        val tacEOptP = propertyStore(method, TACAI.key)
        val tac = if (tacEOptP.hasUBP) tacEOptP.ub.tac else None
        state.tacDependees += method → tacEOptP
        state.tacPCs += method → pcs

        val declaredMethod: DeclaredMethod = declaredMethods(method)
        val callersEOptP = propertyStore(declaredMethod, Callers.key)
        val callers = if (callersEOptP.hasUBP) Some(callersEOptP.ub) else None
        state.callerDependees += declaredMethod -> callersEOptP

        if (tac.isDefined && callers.isDefined) {
            Some((tac.get, callers.get))
        } else None
    }

    /**
     * Determines whether the basic block of a given index dominates the basic block of the other index or is executed
     * before the other index in the case of both indexes belonging to the same basic block.
     */
    def dominates(
        potentiallyDominatorIndex: Int,
        potentiallyDominatedIndex: Int,
        taCode:                    TACode[TACMethodParameter, V]
    ): Boolean = {
        val bbPotentiallyDominator = taCode.cfg.bb(potentiallyDominatorIndex)
        val bbPotentiallyDominated = taCode.cfg.bb(potentiallyDominatedIndex)
        taCode.cfg.dominatorTree.strictlyDominates(bbPotentiallyDominator.nodeId, bbPotentiallyDominated.nodeId) ||
            bbPotentiallyDominator == bbPotentiallyDominated && potentiallyDominatorIndex < potentiallyDominatedIndex
    }

    /**
     * Returns the initialization value of a given type or None if the concrete type can not be determined.
     */
    def getDefaultValues()(implicit state: State): Set[Any] = state.field.fieldType match {
        case FloatType | ObjectType.Float     ⇒ Set(0.0f)
        case DoubleType | ObjectType.Double   ⇒ Set(0.0d)
        case LongType | ObjectType.Long       ⇒ Set(0L)
        case CharType | ObjectType.Character  ⇒ Set('\u0000')
        case BooleanType | ObjectType.Boolean ⇒ Set(false)
        case IntegerType |
            ObjectType.Integer |
            ByteType |
            ObjectType.Byte |
            ShortType |
            ObjectType.Short ⇒ Set(0)
        case ObjectType.String ⇒ Set("", null)
        case _: ReferenceType  ⇒ Set(null)
    }
}
