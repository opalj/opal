/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Representation of the local variable table.
 *
 * @author Michael Eichberg
 */
case class LocalVariableTable(localVariables: LocalVariables) extends CodeAttribute {

    override def kindId: Int = LocalVariableTable.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: LocalVariableTable => this.similar(that)
            case _                        => false
        }
    }

    def similar(other: LocalVariableTable): Boolean = {
        // the order of two local variable tables does not need to be identical
        this.localVariables.size == other.localVariables.size &&
            this.localVariables.forall(other.localVariables.contains)
    }

    override def remapPCs(codeSize: Int, f: PC => PC): CodeAttribute = {
        val newLocalVariables = localVariables.flatMap[LocalVariable](_.remapPCs(codeSize, f))
        LocalVariableTable(newLocalVariables)
    }
}
object LocalVariableTable {

    final val KindId = 20

}
