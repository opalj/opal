/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package instructions

/**
 * Trait that is mixed in by all primitive array access instructions.
 *
 * @author Michael Eichberg
 */
trait PrimitiveArrayAccess extends InstructionMetaInformation

object PrimitiveArrayAccess {

    /**
     * The exceptions that are potentially thrown by instructions that load or store
     * values in an array of primitive values.
     */
    val jvmExceptions: List[ObjectType] = {
        List(ObjectType.ArrayIndexOutOfBoundsException, ObjectType.NullPointerException)
    }

}
