/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.CodeReader

/**
 * This "framework" can be used to read in Java 7 (version 51) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented.
 *
 * @author Michael Eichberg
 */
trait Java7Framework
    extends Java7LibraryFramework
    with CodeAttributeBinding
    with SourceDebugExtension_attributeBinding
    // THOUGH THE BOOTSTRAPMETHODS ATTRIBUTE IS A CLASS-LEVEL ATTRIBUTE
    // IT IS OF NO USE IF WE DO NOT ALSO REIFY THE METHOD BODY
    with BootstrapMethods_attributeBinding
    with StackMapTable_attributeBinding
    with CompactLineNumberTable_attributeBinding
    with LocalVariableTable_attributeBinding
    with LocalVariableTypeTable_attributeBinding
    with Exceptions_attributeBinding
    with BytecodeReaderAndBinding
    with BytecodeOptimizer
    with CodeReader

object Java7Framework extends Java7Framework {

    final override def loadsInterfacesOnly: Boolean = false

}
