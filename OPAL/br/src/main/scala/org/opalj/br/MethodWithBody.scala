/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Provides efficient pattern matching facilities for methods with bodies.
 *
 * @example
 * Matching all methods that have a method body:
 * {{{
 * for {
 *      classFile <- project.classFiles
 *      method @ MethodWithBody(code) <- classFile.methods
 * } {
 *      // the type of method is "..resolved.Method"
 *      // the type of code is "..resolved.Code"
 * }
 * }}}
 *
 * @author Michael Eichberg
 */
object MethodWithBody {

    def unapply(method: Method): Option[Code] = method.body

}
