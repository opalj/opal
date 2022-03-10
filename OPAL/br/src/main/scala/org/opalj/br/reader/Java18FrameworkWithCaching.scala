/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.reader

/**
 * This configuration can be used to read in Java 18 (version 62) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented. Instructions will be cached.
 *
 * @author Julius Naeumann
 */
class Java18FrameworkWithCaching(
        cache: BytecodeInstructionsCache
) extends Java17FrameworkWithCaching(cache) with Java18LibraryFramework
