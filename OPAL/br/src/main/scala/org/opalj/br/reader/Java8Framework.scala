/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This "framework" can be used to read Java 8 (version 52) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented.
 *
 * @author Michael Eichberg
 */
trait Java8Framework extends Java7Framework with Java8LibraryFramework

object Java8Framework extends Java8Framework {

    final override def loadsInterfacesOnly: Boolean = false

}
