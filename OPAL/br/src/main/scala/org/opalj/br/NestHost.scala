/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Definition of a Java 11 nest host.
 *
 * @author Dominik Helm
 */
case class NestHost(hostClassType: ObjectType) extends Attribute {

    final override def kindId: Int = NestHost.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }
}

object NestHost {

    final val KindId = 47

}
