/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias
package pointsto

import org.opalj.br.fpcf.properties.alias.AliasSourceElement
import org.opalj.br.fpcf.properties.pointsto.PointsToSetLike
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Property
import org.opalj.tac.fpcf.analyses.alias.AliasAnalysisContext
import org.opalj.tac.fpcf.analyses.alias.TacBasedAliasAnalysisState

/**
 * Encapsulates the current state of a points-to based alias analysis.
 *
 * It additionally contains the following information:
 *
 * - The current dependees for each of the [[AliasSourceElement]].
 *
 * - The last points-to set for a given points-to entity that has been completely handled for am [[AliasSourceElement]].
 *
 * - The current field dependees for each [[AliasSourceElement]]. A field dependee is a definition site of an uVar that is
 *  used to access the field.
 */
trait PointsToBasedAliasAnalysisState[ElementType, AliasSet <: AliasSetLike[
    ElementType,
    AliasSet
], PointsToSet >: Null <: PointsToSetLike[?, ?, PointsToSet]]
    extends TacBasedAliasAnalysisState
    with SetBasedAliasAnalysisState[ElementType, AliasSet] {

    private var _element1Dependees = Set[Entity]()
    private var _element2Dependees = Set[Entity]()

    private var _oldPointsToSets1: Map[Entity, PointsToSet] = Map.empty[Entity, PointsToSet]
    private var _oldPointsToSets2: Map[Entity, PointsToSet] = Map.empty[Entity, PointsToSet]

    private var _field1Dependees = Set[Entity]()
    private var _field2Dependees = Set[Entity]()

    /**
     * @return A set containing all elements that the first [[AliasSourceElement]] depends on.
     */
    def element1Dependees: Set[Entity] = _element1Dependees

    /**
     * @return A set containing all elements that the second [[AliasSourceElement]] depends on.
     */
    def element2Dependees: Set[Entity] = _element2Dependees

    /**
     * Adds the given entity property pair to the set of dependees of the given [[AliasSourceElement]]
     * and the set of all dependees.
     */
    def addElementDependency(ase: AliasSourceElement, dependency: EOptionP[Entity, Property])(implicit
        context: AliasAnalysisContext
    ): Unit = {

        addDependency(dependency)

        if (context.isElement1(ase)) {
            _element1Dependees += dependency.e
        } else {
            _element2Dependees += dependency.e
        }
    }

    /**
     * Removes the given entity property pair from element dependency set and the set of all dependencies.
     */
    def removeElementDependency(dependency: EOptionP[Entity, Property]): Unit = {

        removeDependency(dependency)

        _element1Dependees -= dependency.e
        _element2Dependees -= dependency.e
    }

    /**
     * @return `true` if the given [[AliasSourceElement]] has a dependency on the given entity property pair.
     */
    def element1HasDependency(dependency: EOptionP[Entity, Property]): Boolean = {
        element1Dependees.contains(dependency.e)
    }

    /**
     * @return `true` if the given [[AliasSourceElement]] has a dependency on the given entity property pair.
     */
    def element2HasDependency(dependency: EOptionP[Entity, Property]): Boolean = {
        element2Dependees.contains(dependency.e)
    }

    /**
     * @return The most recent points-to set of the given points-to entity that has been completely handled for the given [[AliasSourceElement]].
     */
    def oldPointsToSet(ase: AliasSourceElement, e: Entity)(implicit
        context: AliasAnalysisContext
    ): Option[PointsToSet] = {
        if (context.isElement1(ase)) _oldPointsToSets1.get(e)
        else _oldPointsToSets2.get(e)
    }

    /**
     * Updates the points-to set of the given points-to entity that has been completely handled for the given [[AliasSourceElement]].
     */
    def setOldPointsToSet(ase: AliasSourceElement, e: Entity, pointsToSet: PointsToSet)(implicit
        context: AliasAnalysisContext
    ): Unit = {
        if (context.isElement1(ase)) _oldPointsToSets1 += e -> pointsToSet
        else _oldPointsToSets2 += e -> pointsToSet
    }

    /**
     * adds the given entity property pair to the set of field dependees of the first [[AliasSourceElement]].
     */
    def addField1Dependency(dependency: EOptionP[Entity, Property]): Unit = {
        _field1Dependees += dependency.e
    }

    /**
     * adds the given entity property pair to the set of field dependees of the second [[AliasSourceElement]].
     */
    def addField2Dependency(dependency: EOptionP[Entity, Property]): Unit = {
        _field2Dependees += dependency.e
    }

    /**
     * Adds the given entity property pair to the set of field dependees of the given [[AliasSourceElement]].
     */
    def addFieldDependency(ase: AliasSourceElement, dependency: EOptionP[Entity, Property])(implicit
        context: AliasAnalysisContext
    ): Unit = {
        if (context.isElement1(ase)) _field1Dependees += dependency.e
        else _field2Dependees += dependency.e
    }

    /**
     * Removes the given entity property pair from the set of field dependees of all [[AliasSourceElement]]s.
     */
    def removeFieldDependency(dependency: EOptionP[Entity, Property]): Unit = {
        _field1Dependees -= dependency.e
        _field2Dependees -= dependency.e
    }

    /**
     * @return `true` if the first [[AliasSourceElement]] has a field dependency on the given entity property pair.
     */
    def field1HasDependency(dependency: EOptionP[Entity, Property]): Boolean = {
        _field1Dependees.contains(dependency.e)
    }

    /**
     * @return `true` if the second [[AliasSourceElement]] has a field dependency on the given entity property pair.
     */
    def field2HasDependency(dependency: EOptionP[Entity, Property]): Boolean = {
        _field2Dependees.contains(dependency.e)
    }

}
