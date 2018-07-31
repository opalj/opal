/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Definition of a Java 9 module main class.
 *
 * @author Michael Eichberg
 */
case class ModuleMainClass(mainClassType: ObjectType) extends Attribute {

    final override def kindId: Int = ModuleMainClass.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        this == other
    }
}

object ModuleMainClass {

    final val KindId = 45

}
