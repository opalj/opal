/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br

/**
 * Definition of a Java 9 module's packages.
 *
 * @author Michael Eichberg
 */
case class ModulePackages(packages: Packages) extends Attribute {

    final override def kindId: Int = ModulePackages.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        // TODO make the comparisons order independent...
        this == other
    }
}

object ModulePackages {

    final val KindId = 46

}
