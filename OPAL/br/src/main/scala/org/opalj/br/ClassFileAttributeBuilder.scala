/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.collection.immutable.UShortPair

/**
 * Given a class files' main elements the attribute is build.
 *
 * @see The ''BytecodeAssember framework'' for an example usage.
 *
 * @author Michael Eichberg
 */
trait ClassFileAttributeBuilder {

    def apply(
        version:        UShortPair,
        accessFlags:    Int,
        thisType:       ObjectType,
        superclassType: Option[ObjectType],
        interfaceTypes: Seq[ObjectType], // TODO Use a UIDSet over here and in the class hierarchy!
        fields:         Seq[FieldTemplate],
        methods:        Seq[MethodTemplate]
    ): Attribute

}
