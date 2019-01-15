/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.fpcf.PropertyStore

/**
 * This trait is mixed in by those (partial) domains that require access to the project's
 * property store.
 *
 * @author Michael Eichberg
 */
trait ThePropertyStore {

    implicit def propertyStore: PropertyStore
}
