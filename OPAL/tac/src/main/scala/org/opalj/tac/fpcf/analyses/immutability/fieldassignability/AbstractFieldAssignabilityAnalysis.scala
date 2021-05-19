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
import org.opalj.br.fpcf.properties.NotPrematurelyReadField
import org.opalj.br.fpcf.properties.PrematurelyReadField
import org.opalj.br.fpcf.properties.FieldAssignability
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.LBP
import org.opalj.fpcf.Property
import org.opalj.fpcf.UBP
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

    case class State(
            field:                       Field,
            var fieldAssignability:      FieldAssignability                            = EffectivelyNonAssignable,
            var prematurelyReadDependee: Option[EOptionP[Field, FieldPrematurelyRead]] = None,
            var escapeDependees:         Set[EOptionP[DefinitionSite, EscapeProperty]] = Set.empty,
            var tacDependees:            Map[Method, (EOptionP[Method, TACAI], PCs)]   = Map.empty
    ) {
        def hasDependees: Boolean = {
            prematurelyReadDependee.isDefined || escapeDependees.nonEmpty || tacDependees.nonEmpty
        }

        def dependees: Set[EOptionP[Entity, Property]] = {
            (prematurelyReadDependee ++ escapeDependees ++ tacDependees.valuesIterator.map(_._1)).toSet
        }
    }

    /**
     * Returns the TACode for a method if available and registers dependencies if necessary.
     */
    def getTACAI(
        method: Method,
        pcs:    PCs
    )(implicit state: State): Option[TACode[TACMethodParameter, V]] = {
        propertyStore(method, TACAI.key) match {

            case finalEP: FinalEP[Method, TACAI] ⇒ finalEP.ub.tac

            case epk ⇒
                state.tacDependees += method -> ((epk, pcs))
                None
        }
    }

    /**
     * Checks whether the field is prematurely read, i.e. read before it is initialized in the
     * constructor, using the corresponding property.
     */
    def isPrematurelyRead(
        eop: EOptionP[Field, FieldPrematurelyRead]
    )(implicit state: State): Boolean = {
        eop match {

            case LBP(NotPrematurelyReadField) ⇒
                state.prematurelyReadDependee = None
                false

            case UBP(PrematurelyReadField) ⇒
                state.prematurelyReadDependee = None
                true

            case eps ⇒
                state.prematurelyReadDependee = Some(eps)
                true
        }
    }

    /**
     * Determines whether the basic block of a given index dominates the basic block of the other or is executed
     * before the other when the basic blocks are the same
     */
    def dominates(
        potentiallyDominator: Int,
        potentiallyDominated: Int,
        taCode:               TACode[TACMethodParameter, V]
    ): Boolean = {
        val bbPotentiallyDominator = taCode.cfg.bb(potentiallyDominator)
        val bbPotentiallyDominated = taCode.cfg.bb(potentiallyDominated)
        taCode.cfg.dominatorTree.strictlyDominates(bbPotentiallyDominator.nodeId, bbPotentiallyDominated.nodeId) ||
            bbPotentiallyDominator == bbPotentiallyDominated && potentiallyDominator < potentiallyDominated
    }

    /**
     * Returns the value the field will have after initialization or None if there may be multiple
     * values.
     */
    def getDefaultValue()(implicit state: State): Any = {

        state.field.fieldType match {
            case FloatType | ObjectType.Float     ⇒ 0.0f
            case DoubleType | ObjectType.Double   ⇒ 0.0d
            case LongType | ObjectType.Long       ⇒ 0L
            case CharType | ObjectType.Character  ⇒ '\u0000'
            case BooleanType | ObjectType.Boolean ⇒ false
            case IntegerType |
                ObjectType.Integer |
                ByteType |
                ObjectType.Byte |
                ShortType |
                ObjectType.Short ⇒ 0
            case _: ReferenceType ⇒ null
        }
    }
}
