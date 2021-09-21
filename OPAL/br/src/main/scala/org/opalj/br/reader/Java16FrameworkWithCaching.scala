/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This configuration can be used to read in Java 16 (version 60) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented. Instructions will be cached.
 *
 * @author Dominik Helm
 */
class Java16FrameworkWithCaching(
        cache: BytecodeInstructionsCache
) extends Java11FrameworkWithCaching(cache) with Java16LibraryFramework
