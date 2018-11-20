/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.AttributesReader
import org.opalj.bi.reader.SkipUnknown_attributeReader

/**
 * This "framework" can be used to read in Java 7 (version 51) class files if only
 * the public interface of a class is needed.
 *
 * @author Michael Eichberg
 */
trait Java7LibraryFramework
    extends ConstantPoolBinding
    with FieldsBinding
    with MethodsBinding
    with ClassFileBinding
    with AttributesReader
    /* If you want unknown attributes to be represented, uncomment the following: */
    // with Unknown_attributeBinding
    /* and comment out the following line: */
    with SkipUnknown_attributeReader
    with AnnotationAttributesBinding
    with InnerClasses_attributeBinding
    with EnclosingMethod_attributeBinding
    with SourceFile_attributeBinding
    with Deprecated_attributeBinding
    with Signature_attributeBinding
    with Synthetic_attributeBinding
    with ConstantValue_attributeBinding
    with Exceptions_attributeBinding

    // WE HAVE TO LOAD THE JAVA 9 MODULE ATTRIBUTE IN ALL CASES, BECAUSE IT DEFINES THE CRUCIAL
    // INFORMATION REQUIRED TO THE MODULARIZATION OF THE `PROJECT` (THIS IS TRUE EVEN IF WE ARE
    // NOT INTERESTED IN ADVANCED Java8+ ATTRIBUTES...)
    with Module_attributeBinding

object Java7LibraryFramework extends Java7LibraryFramework {

    final override def loadsInterfacesOnly: Boolean = true

}
