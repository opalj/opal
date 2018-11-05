/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.DeclaredMethod
import org.opalj.ai.common.DefinitionSiteLike
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.LocalField
import org.opalj.tac.fpcf.properties.TACAI

class FieldLocalityState(val field: Field) {
    private[this] var declaredMethodsDependees: Set[EOptionP[DeclaredMethod, Property]] = Set.empty
    private[this] var definitionSitesDependees: Map[DefinitionSiteLike, (EOptionP[DefinitionSiteLike, EscapeProperty], Boolean)] = Map.empty
    private[this] var tacDependees: Map[Method, EOptionP[Method, TACAI]] = Map.empty

    private[this] var clonedDependees: Set[DefinitionSiteLike] = Set.empty

    private[this] var temporary: FieldLocality = LocalField

    def dependees: Traversable[EOptionP[Entity, Property]] = {
        (declaredMethodsDependees.iterator ++
            definitionSitesDependees.valuesIterator.map(_._1) ++
            tacDependees.valuesIterator).toTraversable
    }

    def hasNoDependees: Boolean = {
        tacDependees.isEmpty &&
            declaredMethodsDependees.isEmpty &&
            definitionSitesDependees.isEmpty
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

    def addTACDependee(ep: EOptionP[Method, TACAI]): Unit =
        tacDependees += ep.e -> ep

    def removeTACDependee(ep: EOptionP[Method, TACAI]): Unit =
        tacDependees -= ep.e

    def updateWithMeet(f: FieldLocality): Unit = temporary = temporary.meet(f)
    def temporaryState: FieldLocality = temporary
}
