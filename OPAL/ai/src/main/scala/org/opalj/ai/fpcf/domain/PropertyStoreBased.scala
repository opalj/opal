/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package domain

import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionPSet
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.SinglePropertiesBoundType
import org.opalj.ai.domain.TheProject

/**
 * Mixed in by (partial-)domains that query the property store to state the kinds of properties that
 * are accessed.
 *
 * @author Michael Eichberg
 */
trait PropertyStoreBased extends TheProject {

    /**
     * The type of the bound of the properties that are used.
     *
     * @note We are restricted to `SinglePropertiesBoundType` to facilitate matching the bounds.
     */
    val UsedPropertiesBound: SinglePropertiesBoundType

    /**
     * The properties potentially queried by this domain. I.e., it must list '''all properties'''
     * that are potentially queried by any instance.
     *
     * This method '''must call its super method''' and accumulate the results
     * (we have stackable traits!).
     */
    def usesProperties: Set[PropertyKind] = Set.empty

    final def usesPropertyBounds: Set[PropertyBounds] = {
        usesProperties.map(pk => PropertyBounds(UsedPropertiesBound, pk))
    }

    val dependees: EOptionPSet[Entity, Property]

}
