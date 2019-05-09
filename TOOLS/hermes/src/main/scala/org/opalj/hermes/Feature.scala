/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import org.opalj.collection.immutable.Naught
import org.opalj.collection.immutable.Chain

/**
 * Represents the immutable results of a feature query.
 *
 * @param  id A very short identifier of this feature. E.g., `Java8ClassFile` or
 *         `ProtectedMethod` or `DeadMethod`. The name must not contain spaces or other
 *         special characters.
 * @param  count How often the feature was found in a project.
 * @param  extensions Places where the feature was found. This information is
 *         primarily useful when exploring the project and is optional.
 *         I.e., `extensions.size` can be  smaller than `count`. The maximum number
 *         of stored locations is set using the global setting: `org.opalj.hermes.maxLocations`.
 *
 * @author Michael Eichberg
 */
abstract case class Feature[S] private (
        id:         String,
        count:      Int,
        extensions: Chain[Location[S]]
) {
    assert(count >= extensions.size)
}

/**
 * Factory to create features.
 *
 * @author Michael Eichberg
 */
object Feature {

    def apply[S](
        id:         String,
        count:      Int                = 0,
        extensions: Chain[Location[S]] = Naught
    )(
        implicit
        hermes: HermesConfig
    ): Feature[S] = {
        new Feature(id, count, extensions.takeUpTo(hermes.MaxLocations)) {}
    }

    def apply[S](
        id:        String,
        locations: LocationsContainer[S]
    ): Feature[S] = {
        new Feature(id, locations.size, locations.locations) {}
    }
}
