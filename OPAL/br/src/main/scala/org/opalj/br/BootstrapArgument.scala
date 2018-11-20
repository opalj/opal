/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * A marker trait to identify those constant pool values that can be arguments of boot
 * strap methods.
 *
 * @author Michael Eichberg
 */
trait BootstrapArgument {

    def toJava: String

}

