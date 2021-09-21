/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This configuration can be used to read in Java 11 (version 55) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented. Instructions will be cached.
 *
 * @author Dominik Helm
 */
class Java11FrameworkWithCaching(
        cache: BytecodeInstructionsCache
) extends Java9FrameworkWithCaching(cache) with Java11LibraryFramework
