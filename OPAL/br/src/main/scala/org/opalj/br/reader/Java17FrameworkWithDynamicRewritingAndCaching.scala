/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.reader

/**
 * This configuration can be used to read in Java 17 (version 61) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented. Instructions will be cached.
 *
 * @author Dominik Helm
 *         Julius Naeumann
 */
class Java17FrameworkWithDynamicRewritingAndCaching(
        cache: BytecodeInstructionsCache
) extends Java16FrameworkWithDynamicRewritingAndCaching(cache) with Java17LibraryFramework
