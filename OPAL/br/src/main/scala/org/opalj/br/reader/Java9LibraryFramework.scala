/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This "framework" can be used to read in Java 9 (version 53) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented except of method implementations.
 *
 * @author Michael Eichberg
 */
trait Java9LibraryFramework
    extends Java8LibraryFramework
    // with Module_attributeBinding ALREADY MIXED IN (see Java7Framework for details!)
    with ModuleMainClass_attributeBinding
    with ModulePackages_attributeBinding

object Java9LibraryFramework extends Java9LibraryFramework {

    final override def loadsInterfacesOnly: Boolean = true

}
