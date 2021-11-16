/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.reader

/**
 * This configuration can be used to read in Java 17 (version 61) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented except of method implementations.
 *
 * @author Julius Naeumann
 */
trait Java17LibraryFramework
    extends Java16LibraryFramework
    with PermittedSubclasses_attributeBinding

object Java17LibraryFramework extends Java17LibraryFramework {

    final override def loadsInterfacesOnly: Boolean = true

}