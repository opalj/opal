/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.reader

/**
 * This configuration can be used to read in Java 18 (version 62) class files with full
 * support for rewriting `invokedynamic` instructions created by the JDK compiler for
 * lambda and method reference expressions as well as opportunistic support for rewriting dynamic
 * constants. All standard information (as defined in the Java Virtual Machine Specification) is
 * represented. Instructions will be cached.
 *
 * @author Julius Naeumann
 */
class Java18FrameworkWithDynamicRewritingAndCaching(
        cache: BytecodeInstructionsCache
) extends Java17FrameworkWithDynamicRewritingAndCaching(cache) with Java18LibraryFramework
