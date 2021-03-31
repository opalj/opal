/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This configuration can be used to read in Java 11 (version 55) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented.
 *
 * @author Dominik Helm
 */
trait Java11Framework extends Java9Framework with Java11LibraryFramework

object Java11Framework extends Java11Framework {

    final override def loadsInterfacesOnly: Boolean = false

}
