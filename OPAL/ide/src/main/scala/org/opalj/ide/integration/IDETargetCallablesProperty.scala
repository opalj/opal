/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ide
package integration

import scala.collection

import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.fpcf.PropertyKey

/**
 * Property for the target callables that should be analysed by an IDE analysis.
 */
class IDETargetCallablesProperty[Callable <: Entity](
    val key:             PropertyKey[IDETargetCallablesProperty[Callable]],
    val targetCallables: collection.Set[Callable]
) extends Property {
    override type Self = IDETargetCallablesProperty[Callable]

    override def toString: String =
        s"IDETargetCallablesProperty(\n${targetCallables.map { callable => s"\t$callable" }.mkString("\n")}\n)"

    override def equals(other: Any): Boolean = {
        other match {
            case ideTargetCallablesProperty: IDETargetCallablesProperty[?] =>
                key == ideTargetCallablesProperty.key && targetCallables == ideTargetCallablesProperty.targetCallables
            case _ => false
        }
    }

    override def hashCode(): Int = {
        key.hashCode() * 31 + targetCallables.hashCode()
    }
}
