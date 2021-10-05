/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bc

import java.io.DataOutputStream

/**
 * Generic interface which we use to implement the type classes.
 *
 * @author Michael Eichberg
 */
trait ClassFileElement[T] {

    def write(t: T)(implicit out: DataOutputStream, segmentInformation: (String, Int) => Unit): Unit

}

