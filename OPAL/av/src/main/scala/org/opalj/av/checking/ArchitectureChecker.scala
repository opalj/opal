/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import scala.collection.Set

/**
 * An architecture checker validates if the implemented architecture
 * complies with the expected/specified one.
 *
 * @author Marco Torsello
 */
sealed trait ArchitectureChecker {

    def violations(): Set[SpecificationViolation]

}

/**
 * A dependency checker validates if the dependencies between the elements of
 * two ensembles comply with the expected/specified dependencies.
 *
 * @author Michael Eichberg
 * @author Marco Torsello
 */
trait DependencyChecker extends ArchitectureChecker {

    def targetEnsembles: Seq[Symbol]

    def sourceEnsembles: Seq[Symbol]
}

/**
 * A property checker validates if the elements of an ensemble
 * have the expected/specified properties.
 *
 * @author Marco Torsello
 */
trait PropertyChecker extends ArchitectureChecker {

    /**
     * A textual representation of the property.
     */
    def property: String

    def ensembles: Seq[Symbol]
}
