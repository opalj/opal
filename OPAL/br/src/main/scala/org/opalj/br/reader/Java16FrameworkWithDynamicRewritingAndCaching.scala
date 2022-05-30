/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This configuration can be used to read in Java 16 (version 60) class files with full
 * support for rewriting `invokedynamic` instructions created by the JDK compiler for
 * lambda and method reference expressions as well as opportunistic support for rewriting dynamic
 * constants. All standard information (as defined in the Java Virtual Machine Specification) is
 * represented. Instructions will be cached.
 *
 * @author Dominik Helm
 */
class Java16FrameworkWithDynamicRewritingAndCaching(
        cache: BytecodeInstructionsCache
) extends Java11FrameworkWithDynamicRewritingAndCaching(cache) with Java16LibraryFramework
