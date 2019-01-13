/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.fpcf.PropertyBounds

/**
 * Mixed in by (partial-)domains that query the property store to state the kinds of properties that
 * are accessed.
 *
 * @author Michael Eichberg
 */
trait PropertyStoreBased {

    /**
     * The properties potentially queried by this domain. I.e., it must list '''all properties'''
     * that are potentially queried by any instance.
     *
     * This method '''must call its super method''' and accumulate the results
     * (we have stackable traits!).
     */
    def usesPropertyBounds: Set[PropertyBounds] = Set.empty

}
