/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import scala.collection.mutable

import org.opalj.br.DeclaredField
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.FieldLocality
import org.opalj.br.fpcf.properties.LocalField
import org.opalj.br.fpcf.properties.ReturnValueFreshness
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.fieldaccess.AccessParameter
import org.opalj.br.fpcf.properties.fieldaccess.AccessReceiver
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEOptionP
import org.opalj.tac.common.DefinitionSiteLike
import org.opalj.tac.fpcf.properties.TACAI

class FieldLocalityState(val field: Field, val thisIsCloneable: Boolean) {

    private[this] var declaredMethodsDependees: Set[EOptionP[Context, ReturnValueFreshness]] = Set.empty
    private[this] var definitionSitesDependees: Map[
        (Context, DefinitionSiteLike),
        (EOptionP[(Context, DefinitionSiteLike), EscapeProperty], Boolean)
    ] =
        Map.empty
    private[this] var tacDependees: Map[Method, EOptionP[Method, TACAI]] = Map.empty
    private[this] var callerDependees: Map[DeclaredMethod, EOptionP[DeclaredMethod, Callers]] = Map.empty
    private[this] val calleeDependees: mutable.Map[DeclaredMethod, (EOptionP[DeclaredMethod, Callees], IntTrieSet)] =
        mutable.Map()
    private[this] var fieldReadAccessDependee: Option[EOptionP[DeclaredField, FieldReadAccessInformation]] = None
    private[this] var fieldWriteAccessDependee: Option[EOptionP[DeclaredField, FieldWriteAccessInformation]] = None

    var tacFieldReadAccesses: Map[Method, Set[(PC, AccessReceiver)]] = Map.empty
    var tacFieldWriteAccesses: Map[Method, Set[(PC, AccessParameter)]] = Map.empty
    var potentialCloneCallers: Set[Method] = Set.empty
    var overridesClone: Boolean = true

    val thisType: ObjectType = field.classFile.thisType

    private[this] var clonedDependees: Set[(Context, DefinitionSiteLike)] = Set.empty

    private[this] var temporary: FieldLocality = LocalField

    def dependees: Set[SomeEOptionP] = {
        // TODO This really looks very imperformant...
        (declaredMethodsDependees.iterator ++
            fieldReadAccessDependee.filter(_.isRefinable) ++
            fieldWriteAccessDependee.filter(_.isRefinable) ++
            definitionSitesDependees.valuesIterator.map(_._1) ++
            tacDependees.valuesIterator.filter(_.isRefinable) ++
            callerDependees.valuesIterator.filter(_.isRefinable) ++
            calleeDependees.valuesIterator.map(_._1).filter(_.isRefinable)).toSet
    }

    def hasNoDependees: Boolean = {
        fieldReadAccessDependee.forall(_.isFinal) &&
        fieldWriteAccessDependee.forall(_.isFinal) &&
        tacDependees.valuesIterator.forall(_.isFinal) &&
        declaredMethodsDependees.isEmpty &&
        definitionSitesDependees.isEmpty &&
        callerDependees.valuesIterator.forall(_.isFinal) &&
        calleeDependees.valuesIterator.forall(_._1.isFinal)
    }

    def hasTacDependees: Boolean = tacDependees.valuesIterator.exists(_.isRefinable)

    def isDefinitionSiteOfClone(e: (Context, DefinitionSiteLike)): Boolean =
        clonedDependees.contains(e)

    def addMethodDependee(ep: EOptionP[Context, ReturnValueFreshness]): Unit =
        declaredMethodsDependees += ep

    def removeMethodDependee(ep: EOptionP[Context, ReturnValueFreshness]): Unit =
        declaredMethodsDependees = declaredMethodsDependees.filter(_.e ne ep.e)

    def updateMethodDependee(ep: EOptionP[Context, ReturnValueFreshness]): Unit = {
        removeMethodDependee(ep)
        addMethodDependee(ep)
    }

    def addClonedDefinitionSiteDependee(
        ep: EOptionP[(Context, DefinitionSiteLike), EscapeProperty]
    ): Unit = {
        addDefinitionSiteDependee(ep, isGetFieldOfReceiver = false)
        clonedDependees += ep.e
    }

    def addDefinitionSiteDependee(
        ep:                   EOptionP[(Context, DefinitionSiteLike), EscapeProperty],
        isGetFieldOfReceiver: Boolean
    ): Unit = {
        definitionSitesDependees += ep.e -> ((ep, isGetFieldOfReceiver))
    }

    def removeDefinitionSiteDependee(
        ep: EOptionP[(Context, DefinitionSiteLike), EscapeProperty]
    ): Unit = {
        definitionSitesDependees -= ep.e
    }

    def isGetFieldOfReceiver(defSite: (Context, DefinitionSiteLike)): Boolean = {
        definitionSitesDependees(defSite)._2
    }

    def updateAllocationSiteDependee(
        ep: EOptionP[(Context, DefinitionSiteLike), EscapeProperty]
    ): Unit = {
        val getFieldOfReceiver = isGetFieldOfReceiver(ep.e)
        removeDefinitionSiteDependee(ep)
        addDefinitionSiteDependee(ep, getFieldOfReceiver)
    }

    def addTACDependee(ep: EOptionP[Method, TACAI]): Unit = {
        tacDependees += ep.e -> ep
    }

    def getTACDependee(m: Method): Option[EOptionP[Method, TACAI]] = {
        tacDependees.get(m)
    }

    def addCallersDependee(ep: EOptionP[DeclaredMethod, Callers]): Unit = {
        callerDependees += ep.e -> ep
    }

    def getCallersDependee(dm: DeclaredMethod): Option[EOptionP[DeclaredMethod, Callers]] = {
        callerDependees.get(dm)
    }

    def setFieldReadAccessDependee(ep: EOptionP[DeclaredField, FieldReadAccessInformation]): Unit = {
        fieldReadAccessDependee = Some(ep)
    }

    def getFieldReadAccessDependee: Option[EOptionP[DeclaredField, FieldReadAccessInformation]] =
        fieldReadAccessDependee

    def setFieldWriteAccessDependee(ep: EOptionP[DeclaredField, FieldWriteAccessInformation]): Unit = {
        fieldWriteAccessDependee = Some(ep)
    }

    def getFieldWriteAccessDependee: Option[EOptionP[DeclaredField, FieldWriteAccessInformation]] =
        fieldWriteAccessDependee

    def addCallsite(
        caller: DeclaredMethod,
        pc:     PC
    )(implicit propertyStore: PropertyStore): EOptionP[DeclaredMethod, Callees] = {
        if (calleeDependees.contains(caller)) {
            val (oldEP, pcs) = calleeDependees(caller)
            if (!pcs.contains(pc))
                calleeDependees.update(caller, (oldEP, pcs + pc))
            oldEP
        } else {
            val ep = propertyStore(caller, Callees.key)
            calleeDependees.put(caller, (ep, IntTrieSet(pc)))
            ep
        }
    }

    def updateCalleeDependee(ep: EOptionP[DeclaredMethod, Callees]): Unit = {
        val pcs = getCallsites(ep.e)
        calleeDependees.update(ep.e, (ep, pcs))
    }

    def getCallsites(caller: DeclaredMethod): IntTrieSet = {
        calleeDependees(caller)._2
    }

    def updateWithMeet(f: FieldLocality): Unit = temporary = temporary.meet(f)
    def temporaryState: FieldLocality = temporary
}
