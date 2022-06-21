/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Representation of the local variable type table.
 *
 * @author Michael Eichberg
 */
case class LocalVariableTypeTable(localVariableTypes: LocalVariableTypes) extends CodeAttribute {

    override def kindId: Int = LocalVariableTypeTable.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: LocalVariableTypeTable => this.similar(that)
            case _                            => false
        }
    }

    def similar(other: LocalVariableTypeTable): Boolean = {
        // the order of two local variable type tables does not need to be identical
        this.localVariableTypes.size == other.localVariableTypes.size &&
            this.localVariableTypes.forall(other.localVariableTypes.contains)
    }

    override def remapPCs(codeSize: Int, f: PC => PC): CodeAttribute = {
        LocalVariableTypeTable(
            localVariableTypes.flatMap[LocalVariableType](_.remapPCs(codeSize, f))
        )
    }
}
object LocalVariableTypeTable {

    final val KindId = 21

}

case class LocalVariableType(
        startPC:   PC,
        length:    Int,
        name:      String,
        signature: FieldTypeSignature,
        index:     Int
) {
    def remapPCs(codeSize: Int, f: PC => PC): Option[LocalVariableType] = {
        val newStartPC = f(startPC)
        if (newStartPC < codeSize)
            Some(
                LocalVariableType(
                    newStartPC,
                    f(startPC + length) - newStartPC,
                    name,
                    signature,
                    index
                )
            )
        else
            None
    }
}
