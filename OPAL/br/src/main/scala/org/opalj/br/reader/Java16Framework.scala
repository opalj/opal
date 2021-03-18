/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This configuration can be used to read in Java 1t (version 60) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented.
 *
 * @author Dominik Helm
 */
trait Java16Framework extends Java11Framework with Java16LibraryFramework

object Java16Framework extends Java16Framework {

    final override def loadsInterfacesOnly: Boolean = false

}
