/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package cp

/**
 * Constant pool entry that represents method refs.
 *
 * The created `MethodRef` is cached.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
trait AsMethodref extends Constant_Pool_Entry {

    def class_index: Constant_Pool_Index

    def name_and_type_index: Constant_Pool_Index

    def isInterfaceMethodRef: Boolean

    // to cache the result
    @volatile private[this] var methodref: (ReferenceType, Boolean, String, MethodDescriptor) = null
    override def asMethodref(
        cp: Constant_Pool
    ): (ReferenceType, Boolean /* isInterface*/ , String, MethodDescriptor) = {
        // The following solution is sufficiently thread safe; i.e.,
        // it may happen that two or more methodref instances
        // are created, but these instances are guaranteed to
        // be equal (`==`).

        var methodref = this.methodref
        if (methodref eq null) {
            val nameAndType = cp(name_and_type_index).asNameAndType
            methodref =
                (
                    cp(class_index).asReferenceType(cp),
                    isInterfaceMethodRef,
                    nameAndType.name(cp),
                    nameAndType.methodDescriptor(cp)
                )
            this.methodref = methodref
        }
        methodref
    }
}
