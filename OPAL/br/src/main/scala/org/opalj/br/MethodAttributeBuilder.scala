/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Given a method's signature and access flags the attribute related to the method is build.
 *
 * @see The BytecodeAssember framework for an example usage.
 *
 * @author Michael Eichberg
 */
trait MethodAttributeBuilder {

    def apply(
        accessFlags: Int,
        name:        String,
        descriptor:  MethodDescriptor
    ): Attribute

}
