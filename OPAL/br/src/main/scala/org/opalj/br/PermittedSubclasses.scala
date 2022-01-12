/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br

/**
 * Definition of Java 17 permitted subclasses of a sealed class.
 *
 * @author Julius Naeumann
 */
case class PermittedSubclasses(classes: Classes) extends Attribute {

    final override def kindId: Int = PermittedSubclasses.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }
}

object PermittedSubclasses {

    final val KindId = 50

}