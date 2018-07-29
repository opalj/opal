/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Java 7's 'BootstrapMethod'.
 *
 * @author Michael Eichberg
 */
case class BootstrapMethod(
        handle:    MethodHandle,
        arguments: BootstrapArguments
) {

    def toJava: String = arguments.map(_.toJava).mkString(handle.toJava+"(", ",", ")")
}
