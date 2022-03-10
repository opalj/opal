/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.reader

/**
 * This configuration can be used to read in Java 18 (version 62) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented except of method implementations.
 *
 * @author Julius Naeumann
 */
trait Java18LibraryFramework
    extends Java17LibraryFramework

object Java18LibraryFramework extends Java18LibraryFramework {

    final override def loadsInterfacesOnly: Boolean = true

}

