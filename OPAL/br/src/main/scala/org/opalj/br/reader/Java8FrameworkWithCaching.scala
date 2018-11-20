/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.CodeReader

/**
 * This configuration can be used to read in Java 8 (version 52) class files. All
 * standard information (as defined in the Java Virtual Machine Specification)
 * is represented. Instructions will be cached.
 *
 * @author Michael Eichberg
 */
class Java8FrameworkWithCaching(
        val cache: BytecodeInstructionsCache
) extends Java8LibraryFramework
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
    with CachedBytecodeReaderAndBinding
    with BytecodeOptimizer
    with CodeReader {

    final override def loadsInterfacesOnly: Boolean = false
}
