/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.DeclaredMethod
import org.opalj.ai.common.DefinitionSiteLike
import org.opalj.br.Field
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.LocalField

class FieldLocalityState(val field: Field) {

    private[this] var declaredMethodsDependees: Set[EOptionP[DeclaredMethod, Property]] = Set.empty
    private[this] var definitionSitesDependees: Set[EOptionP[DefinitionSiteLike, EscapeProperty]] = Set.empty

    private[this] var clonedDependees: Set[DefinitionSiteLike] = Set.empty

    private[this] var temporary: FieldLocality = LocalField

    def dependees: Set[EOptionP[Entity, Property]] = {
        declaredMethodsDependees ++ definitionSitesDependees
    }

    def hasNoDependees: Boolean = {
        declaredMethodsDependees.isEmpty &&
            definitionSitesDependees.isEmpty
    }
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
        addDefinitionSiteDependee(ep)
        clonedDependees += ep.e
    }

    def addDefinitionSiteDependee(ep: EOptionP[DefinitionSiteLike, EscapeProperty]): Unit =
        definitionSitesDependees += ep

    def removeDefinitionSiteDependee(ep: EOptionP[DefinitionSiteLike, EscapeProperty]): Unit =
        definitionSitesDependees = definitionSitesDependees.filter(other ⇒ (other.e ne ep.e) || other.pk != ep.pk)

    def updateAllocationSiteDependee(ep: EOptionP[DefinitionSiteLike, EscapeProperty]): Unit = {
        removeDefinitionSiteDependee(ep)
        addDefinitionSiteDependee(ep)
    }

    def updateWithMeet(f: FieldLocality): Unit = temporary = temporary.meet(f)
    def temporaryState: FieldLocality = temporary
}
