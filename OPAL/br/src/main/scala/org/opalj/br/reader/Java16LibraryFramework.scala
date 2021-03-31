/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This configuration can be used to read in Java 16 (version 60) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented except of method implementations.
 *
 * @author Dominik Helm
 */
trait Java16LibraryFramework
    extends Java11LibraryFramework
    with Record_attributeBinding

object Java16LibraryFramework extends Java16LibraryFramework {

    final override def loadsInterfacesOnly: Boolean = true

}
