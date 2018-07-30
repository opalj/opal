/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This configuration can be used to read in Java 9 (version 53) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented. Instructions will be cached.
 *
 * @author Michael Eichberg
 */
class Java9FrameworkWithCaching(
        cache: BytecodeInstructionsCache
) extends Java8FrameworkWithCaching(cache)
    with Java9LibraryFramework
