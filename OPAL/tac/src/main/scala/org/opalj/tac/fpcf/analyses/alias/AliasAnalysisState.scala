/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package alias

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Property
import org.opalj.fpcf.SomeEOptionP

/**
 * Encapsulates the current state of an alias analysis.
 *
 * It handles the current dependees of the analysis.
 *
 * It can be overridden to provide additional state information to the computation.
 */
trait AliasAnalysisState {

    private[this] var _dependees = Map.empty[Entity, EOptionP[Entity, Property]]
    private[this] var _dependeesSet = Set.empty[SomeEOptionP]

    /**
     * Adds the given entity property pair to the set of dependees.
     * @param dependency The entity property pair to add.
     */
    @inline private[alias] final def addDependency(dependency: EOptionP[Entity, Property]): Unit = {
        _dependees += dependency.e -> dependency
        _dependeesSet += dependency
    }

    /**
     * Removes the given entity property pair from the set of dependees.
     */
    @inline private[alias] final def removeDependency(dependency: EOptionP[Entity, Property]): Unit = {
        val oldEOptionP = _dependees(dependency.e)
        _dependees -= dependency.e
        _dependeesSet -= oldEOptionP
    }

    /**
     * @return A set containing all dependees.
     */
    private[alias] final def getDependees: Set[SomeEOptionP] = {
        _dependeesSet
    }

    /**
     * @return `true` if there are any dependees, `false` otherwise.
     */
    private[alias] final def hasDependees: Boolean = _dependees.nonEmpty

}
