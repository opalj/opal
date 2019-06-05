/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import scala.collection.mutable

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Property
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.PC
import org.opalj.br.PCs
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.FieldLocality
import org.opalj.br.fpcf.properties.LocalField
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.tac.common.DefinitionSiteLike
import org.opalj.tac.fpcf.properties.TACAI

class FieldLocalityState(val field: Field, val thisIsCloneable: Boolean) {

    private[this] var declaredMethodsDependees: Set[EOptionP[DeclaredMethod, Property]] = Set.empty
    private[this] var definitionSitesDependees: Map[DefinitionSiteLike, (EOptionP[DefinitionSiteLike, EscapeProperty], Boolean)] = Map.empty
    private[this] var tacDependees: Map[Method, EOptionP[Method, TACAI]] = Map.empty
    private[this] var calleeDependees: mutable.Map[DeclaredMethod, (EOptionP[DeclaredMethod, Callees], IntTrieSet)] = mutable.Map()

    var tacFieldAccessPCs: Map[Method, PCs] = Map.empty
    var potentialCloneCallers: Set[Method] = Set.empty
    var overridesClone: Boolean = true

    val thisType = field.classFile.thisType

    private[this] var clonedDependees: Set[DefinitionSiteLike] = Set.empty

    private[this] var temporary: FieldLocality = LocalField

    def dependees: Traversable[EOptionP[Entity, Property]] = {
        // TODO This really looks very imperformant...
        (declaredMethodsDependees.iterator ++
            definitionSitesDependees.valuesIterator.map(_._1) ++
            tacDependees.valuesIterator ++
            calleeDependees.valuesIterator.map(_._1).filter(_.isRefinable)).toTraversable
    }

    def hasNoDependees: Boolean = {
        tacDependees.isEmpty &&
            declaredMethodsDependees.isEmpty &&
            definitionSitesDependees.isEmpty &&
            calleeDependees.valuesIterator.forall(_._1.isFinal)
    }

    def hasTacDependees: Boolean = tacDependees.nonEmpty

    def isDefinitionSiteOfClone(e: DefinitionSiteLike): Boolean =
        clonedDependees.contains(e)

    def addMethodDependee(ep: EOptionP[DeclaredMethod, Property]): Unit =
        declaredMethodsDependees += ep

    def removeMethodDependee(ep: EOptionP[DeclaredMethod, Property]): Unit =
        declaredMethodsDependees = declaredMethodsDependees.filter(other ⇒ (other.e ne ep.e) || other.pk != ep.pk)

    def updateMethodDependee(ep: EOptionP[DeclaredMethod, Property]): Unit = {
        removeMethodDependee(ep)
        addMethodDependee(ep)
    }

    def addClonedDefinitionSiteDependee(ep: EOptionP[DefinitionSiteLike, EscapeProperty]): Unit = {
        addDefinitionSiteDependee(ep, isGetFieldOfReceiver = false)
        clonedDependees += ep.e
    }

    def addDefinitionSiteDependee(
        ep:                   EOptionP[DefinitionSiteLike, EscapeProperty],
        isGetFieldOfReceiver: Boolean
    ): Unit = {
        definitionSitesDependees += ep.e → ((ep, isGetFieldOfReceiver))
    }

    def removeDefinitionSiteDependee(ep: EOptionP[DefinitionSiteLike, EscapeProperty]): Unit = {
        definitionSitesDependees -= ep.e
    }

    def isGetFieldOfReceiver(defSite: DefinitionSiteLike): Boolean = {
        definitionSitesDependees(defSite)._2
    }

    def updateAllocationSiteDependee(ep: EOptionP[DefinitionSiteLike, EscapeProperty]): Unit = {
        val getFieldOfReceiver = isGetFieldOfReceiver(ep.e)
        removeDefinitionSiteDependee(ep)
        addDefinitionSiteDependee(ep, getFieldOfReceiver)
    }

    def addTACDependee(ep: EOptionP[Method, TACAI]): Unit = {
        tacDependees += ep.e → ep
    }

    def addTACDependee(ep: EOptionP[Method, TACAI], pcs: PCs): Unit = {
        tacDependees += ep.e → ep
        tacFieldAccessPCs += ep.e → (tacFieldAccessPCs.getOrElse(ep.e, IntTrieSet.empty) ++ pcs)
    }

    def removeTACDependee(ep: EOptionP[Method, TACAI]): Unit = {
        tacDependees -= ep.e
    }

    def addCallsite(
        ep: EOptionP[DeclaredMethod, Callees],
        pc: PC
    ): EOptionP[DeclaredMethod, Callees] = {
        val caller = ep.e
        if (calleeDependees.contains(caller)) {
            val (oldEP, pcs) = calleeDependees(caller)
            if (!pcs.contains(pc))
                calleeDependees.update(caller, (oldEP, pcs + pc))
            oldEP
        } else {
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
