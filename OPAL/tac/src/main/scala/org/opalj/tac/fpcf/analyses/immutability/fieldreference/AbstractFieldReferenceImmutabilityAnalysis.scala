/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability
package fieldreference

import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimUBP
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.PCs
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.FieldPrematurelyRead
import org.opalj.br.fpcf.properties.ImmutableFieldReference
import org.opalj.br.fpcf.properties.MutableFieldReference
import org.opalj.br.fpcf.properties.NotPrematurelyReadField
import org.opalj.br.fpcf.properties.PrematurelyReadField
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.properties.FieldReferenceImmutability
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.br.fpcf.properties.cg.Callees
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

/**
 *
 * Encompasses the base functions for determining field reference immutability.
 *
 * @author Tobias Roth
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 */
trait AbstractFieldReferenceImmutabilityAnalysis extends FPCFAnalysis {

    type V = DUVar[ValueInformation]
    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)
    final val definitionSites = project.get(DefinitionSitesKey)
    implicit final val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    case class State(
            field:                              Field,
            var referenceImmutability:          FieldReferenceImmutability                                         = ImmutableFieldReference,
            var prematurelyReadDependee:        Option[EOptionP[Field, FieldPrematurelyRead]]                      = None,
            var purityDependees:                Set[EOptionP[DeclaredMethod, Purity]]                              = Set.empty,
            var lazyInitInvocation:             Option[(DeclaredMethod, PC)]                                       = None,
            var calleesDependee:                Map[DeclaredMethod, (EOptionP[DeclaredMethod, Callees], List[PC])] = Map.empty,
            var referenceImmutabilityDependees: Set[EOptionP[Field, FieldReferenceImmutability]]                   = Set.empty,
            var escapeDependees:                Set[EOptionP[DefinitionSite, EscapeProperty]]                      = Set.empty,
            var tacDependees:                   Map[Method, (EOptionP[Method, TACAI], PCs)]                        = Map.empty,
            var typeDependees:                  Set[EOptionP[ObjectType, TypeImmutability]]                        = Set.empty
    ) {
        def hasDependees: Boolean = {
            prematurelyReadDependee.isDefined || purityDependees.nonEmpty || prematurelyReadDependee.isDefined ||
                purityDependees.nonEmpty || calleesDependee.nonEmpty || referenceImmutabilityDependees.nonEmpty ||
                escapeDependees.nonEmpty || tacDependees.nonEmpty || typeDependees.nonEmpty
        }

        def dependees: Traversable[EOptionP[Entity, Property]] = {
            prematurelyReadDependee ++ purityDependees ++ referenceImmutabilityDependees ++ escapeDependees ++
                calleesDependee.valuesIterator.map(_._1) ++ tacDependees.valuesIterator.map(_._1) ++ typeDependees
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
     * Checks if the method that defines the value assigned to a (potentially) lazily initialized
     * field is deterministic, ensuring that the same value is written even for concurrent
     * executions.
     */
    def isNonDeterministic(
        eop: EOptionP[DeclaredMethod, Purity]
    )(implicit state: State): Boolean = eop match {
        case LBP(p: Purity) if p.isDeterministic  ⇒ false
        case UBP(p: Purity) if !p.isDeterministic ⇒ true

        case _ ⇒
            state.purityDependees += eop
            false
    }

    def lazyInitializerIsDeterministic(
        method: Method
    )(implicit state: State): Boolean = {
        //over-approximates that the lazy initialization can not be influenced via a parameter
        if (method.descriptor.parametersCount > 0) false
        else !isNonDeterministic(propertyStore(declaredMethods(method), Purity.key))
    }

    /**
     * Checks if the field the value assigned to a (potentially) lazily initialized field is final,
     * ensuring that the same value is written even for concurrent executions.
     */
    def isImmutableReference(
        eop: EOptionP[Field, FieldReferenceImmutability]
    )(implicit state: State): Boolean = eop match {

        case LBP(ImmutableFieldReference) ⇒ true
        case UBP(MutableFieldReference)   ⇒ false

        case _ ⇒
            state.referenceImmutabilityDependees += eop
            true
    }

    /**
     * Checks if the field is prematurely read, i.e. read before it is initialized in the
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
                false
        }
    }

    /**
     * Checks if the calls at a given pc within a given method introduce non determinism.
     * @return if the calls introduce nondeterminism
     */
    def doCallsIntroduceNonDeterminism(
        calleesEOP: EOptionP[DeclaredMethod, Callees],
        pc:         PC
    )(implicit state: State): Boolean = {
        calleesEOP match {

            case FinalP(callees)     ⇒ isNonDeterministicCallee(callees, pc)
            case InterimUBP(callees) ⇒ isNonDeterministicCallee(callees.asInstanceOf[Callees], pc)

            case _ ⇒
                state.calleesDependee += calleesEOP.e → ((calleesEOP, pc :: state.calleesDependee(calleesEOP.e)._2))
                false
        }
    }

    /**
     * Checks if a callee is nondeterministic and sets the [[State.referenceImmutability]]
     * if it is to MutableFieldReference.
     * @return if the callee is nondeterministic
     */
    def isNonDeterministicCallee(callees: Callees, pc: PC)(implicit state: State): Boolean = {
        if (callees.isIncompleteCallSite(pc)) {
            state.referenceImmutability = MutableFieldReference
            true
        } else {
            val targets = callees.callees(pc)
            if (targets.exists(target ⇒ isNonDeterministic(propertyStore(target, Purity.key)))) {
                state.referenceImmutability = MutableFieldReference
                true
            } else false
        }
    }
}
