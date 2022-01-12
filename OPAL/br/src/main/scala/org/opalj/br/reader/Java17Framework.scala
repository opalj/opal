/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.reader

/**
 * This configuration can be used to read in Java 1t (version 60) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented.
 *
 * @author Julius Naeumann
 */
trait Java17Framework extends Java16Framework with Java17LibraryFramework

object Java17Framework extends Java17Framework {

    final override def loadsInterfacesOnly: Boolean = false

}