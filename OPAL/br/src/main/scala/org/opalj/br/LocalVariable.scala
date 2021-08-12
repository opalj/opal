/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * An entry in a local variable table.
 *
 * @author Michael Eichberg
 */
case class LocalVariable(
        startPC:   PC,
        length:    Int,
        name:      String,
        fieldType: FieldType,
        index:     Int
) {

    def remapPCs(codeSize: Int, f: PC => PC): Option[LocalVariable] = {
        val newStartPC = f(startPC)
        if (newStartPC < codeSize)
            Some(
                LocalVariable(
                    newStartPC,
                    f(startPC + length) - newStartPC,
                    name,
                    fieldType,
                    index
                )
            )
        else
            None
    }

}
