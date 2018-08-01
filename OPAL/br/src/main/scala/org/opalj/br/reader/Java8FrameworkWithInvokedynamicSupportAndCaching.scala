/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This configuration can be used to read in Java 8 (version 52) class files with full
 * support for rewriting `invokedynamic` instructions created by the JDK(8) compiler for
 * lambda and method reference expressions. All standard information (as defined in the
 * Java Virtual Machine Specification) is represented. Instructions will be cached.
 *
 * @author Arne Lottmann
 * @author Michael Eichberg
 */
class Java8FrameworkWithInvokedynamicSupportAndCaching(
        cache: BytecodeInstructionsCache
) extends Java8FrameworkWithCaching(cache)
    with InvokedynamicRewriting
