/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContexts

/**
 * Attribute in a class' attribute table that encodes information about inner classes.
 *
 * @author Michael Eichberg
 */
case class InnerClassTable(innerClasses: InnerClasses) extends Attribute {

    override def kindId: Int = InnerClassTable.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        other match {
            case that: InnerClassTable => this.similar(that)
            case _                     => false
        }
    }

    def similar(other: InnerClassTable): Boolean = {
        // the order of two inner classes tables does not need to be identical
        this.innerClasses.size == other.innerClasses.size &&
            this.innerClasses.forall(other.innerClasses.contains)
    }

}
object InnerClassTable {

    final val KindId = 9

}

case class InnerClass(
        innerClassType:        ObjectType,
        outerClassType:        Option[ObjectType],
        innerName:             Option[String],
        innerClassAccessFlags: Int
) {

    override def toString(): String = {
        "InnerClass"+
            "(type="+innerClassType.toJava+
            ",outerType="+outerClassType.map(_.toJava)+
            ",innerName="+innerName+
            ",accessFlags=\""+AccessFlags.toString(innerClassAccessFlags, AccessFlagsContexts.INNER_CLASS)+"\""+
            ")"
    }
}
