/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Attribute in a method's attributes table that declares the (checked) exceptions
 * that may be thrown by the method.
 *
 * @author Michael Eichberg
 */
case class ExceptionTable(exceptions: Exceptions) extends Attribute {

    override def kindId: Int = ExceptionTable.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: ExceptionTable => this.similar(that)
            case _                    => false
        }
    }

    def similar(other: ExceptionTable): Boolean = {
        // the order does not have to be identical "... throws IOException, Throwable"
        // is the same as "... throws Throwable, IOException"
        this.exceptions.size == other.exceptions.size &&
            this.exceptions.iterator.zip(other.exceptions.iterator).forall { e =>
                val (thisEx, otherEx) = e
                thisEx == otherEx
            }
    }

}
object ExceptionTable {

    final val KindId = 8

}
