/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

/**
 * Default class file binding where all private fields and methods are not represented.
 *
 * @author Michael Eichberg
 */
trait LibraryClassFileBinding extends ClassFileBinding {
    this: ConstantPoolBinding with MethodsBinding with FieldsBinding with AttributeBinding =>

    override def ClassFile(
        cp:                Constant_Pool,
        minor_version:     Int,
        major_version:     Int,
        access_flags:      Int,
        this_class_index:  Constant_Pool_Index,
        super_class_index: Constant_Pool_Index,
        interfaces:        Interfaces,
        fields:            Fields,
        methods:           Methods,
        attributes:        Attributes
    ): ClassFile = {
        super.ClassFile(
            cp,
            minor_version, major_version,
            access_flags,
            this_class_index,
            super_class_index,
            interfaces,
            fields.filterNot(_.isPrivate),
            methods.filterNot(_.isPrivate),
            attributes
        )
    }
}
