/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.collection.immutable.UShortPair

/**
 * Given a method's signature and access flags the code attribute is build
 * and some meta information - depending on the type of the code attribute builder - is collected.
 *
 * @see The BytecodeAssember framework for an example usage.
 * @author Michael Eichberg
 */
trait CodeAttributeBuilder[T] {

    /**
     * @param classFileVersion The class file version determines which attributes are allowed/
     *                         required. In particular required to determine if a
     *                         [[org.opalj.br.StackMapTable]] attribute needs to be computed.
     * @param accessFlags The access flags; required to compute max locals if necessary
     *                    (static or not?).
     * @param name The name of the method.
     * @param descriptor The method's descriptor; required to compute max locals if necessary.
     * @param classHierarchy Required if a new [[org.opalj.br.StackMapTable]] attribute needs
     *                       to be computed.
     * @return The newly build code attribute.
     */
    def apply(
        classFileVersion:   UShortPair,
        declaringClassType: ObjectType,
        accessFlags:        Int,
        name:               String,
        descriptor:         MethodDescriptor
    )(
        implicit
        classHierarchy: ClassHierarchy
    ): (Attribute, T)

}
