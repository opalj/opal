/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This "framework" can be used to read Java 9 (version 53) and Java 10 (version 54) class files.
 * All standard information (as defined in the Java Virtual Machine Specification) is represented.
 * (Java 10 did not introduce new attributes.)
 *
 * @author Michael Eichberg
 */
trait Java9Framework extends Java8Framework with Java9LibraryFramework

object Java9Framework extends Java9Framework {

    final override def loadsInterfacesOnly: Boolean = false

}
