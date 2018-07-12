/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

/**
 * Configures how specific kinds of attributes are handled during the serialization process.
 *
 * @author Michael Eichberg
 */
case class ToDAConfig(
        retainOPALAttributes:    Boolean = false,
        retainUnknownAttributes: Boolean = false
)

object ToDAConfig {
    val RetainAllAttributes: ToDAConfig = ToDAConfig(true, true)
}
