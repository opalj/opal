/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.ifds

abstract class Callable {
    /**
     * The name of the Callable
     */
    def name: String

    /**
     * The full name of the Callable including its signature
     */
    def signature: String
}
