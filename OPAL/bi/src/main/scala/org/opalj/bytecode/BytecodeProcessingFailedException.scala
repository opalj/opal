/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.bytecode

/**
 * Indicates that the processing of a class file failed. The reason is either a bug in the
 * framework or in the class file.
 *
 * @note    The '''Eclipse Luna''' Java compiler does not generate valid class files in a few cases
 *          where type annotations are used in combination with try-with-resources statements.
 * @note    The '''Scala compiler''' - at least up to version 2.12.5 - sometimes generates invalid
 *          type signatures in the combination with parameterized value types.
 *
 * @author Michael Eichberg
 */
case class BytecodeProcessingFailedException(message: String) extends RuntimeException(message)
