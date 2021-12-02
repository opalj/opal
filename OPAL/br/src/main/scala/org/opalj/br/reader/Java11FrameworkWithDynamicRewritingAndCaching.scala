/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This configuration can be used to read in Java 11 (version 55) class files with full
 * support for rewriting `invokedynamic` instructions created by the JDK compiler for
 * lambda and method reference expressions as well as opportunistic support for rewriting dynamic
 * constants. All standard information (as defined in the Java Virtual Machine Specification) is
 * represented. Instructions will be cached.
 *
 * @author Dominik Helm
 */
class Java11FrameworkWithDynamicRewritingAndCaching(
        cache: BytecodeInstructionsCache
) extends Java9FrameworkWithInvokedynamicSupportAndCaching(cache)
    with Java11LibraryFramework
    with DynamicConstantRewriting
