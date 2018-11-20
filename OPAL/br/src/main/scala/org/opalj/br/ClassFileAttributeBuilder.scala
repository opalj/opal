/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.collection.immutable.UShortPair
import org.opalj.collection.immutable.RefArray

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
        interfaceTypes: RefArray[ObjectType],
        fields:         RefArray[FieldTemplate],
        methods:        RefArray[MethodTemplate]
    ): Attribute

}
