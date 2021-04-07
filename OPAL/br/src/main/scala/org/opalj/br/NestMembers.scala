/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Definition of Java 11 nest members.
 *
 * @author Dominik Helm
 */
case class NestMembers(classes: Classes) extends Attribute {

    final override def kindId: Int = NestMembers.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }
}

object NestMembers {

    final val KindId = 48

}
