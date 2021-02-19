/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.reader

/**
 * This configuration can be used to read in Java 11 (version 55) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented except of method implementations.
 *
 * @author Michael Eichberg
 */
trait Java11LibraryFramework
    extends Java9LibraryFramework
    with NestHost_attributeBinding
    with NestMembers_attributeBinding

