/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * This "framework" can be used to read in Java 8 (version 52) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented except of method implementations.
 *
 * @author Michael Eichberg
 */
trait Java8LibraryFramework
    extends Java7LibraryFramework
    with MethodParameters_attributeBinding
    with TypeAnnotationAttributesBinding

object Java8LibraryFramework extends Java8LibraryFramework {

    // IMPROVE Extend the infrastructure to only read the non-private methods and fields.
    final override def loadsInterfacesOnly: Boolean = true

}
