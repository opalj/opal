/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This configuration can be used to read in Java 9 (version 53) class files with full
 * support for rewriting `invokedynamic` instructions created by the JDK compiler for
 * lambda and method reference expressions. All standard information (as defined in the
 * Java Virtual Machine Specification) is represented. Instructions will be cached.
 *
 * @author Michael Eichberg
 */
class Java9FrameworkWithInvokedynamicSupportAndCaching(
        cache: BytecodeInstructionsCache
) extends Java8FrameworkWithInvokedynamicSupportAndCaching(cache) with Java9LibraryFramework
