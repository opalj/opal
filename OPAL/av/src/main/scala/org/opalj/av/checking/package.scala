/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av

import scala.language.implicitConversions

/**
 * Helper classes and functionality related to specifying architectural concerns.
 *
 * @author Michael Eichberg
 */
package object checking {

    implicit def StringToBinaryString(string: String): BinaryString = BinaryString(string)

}
