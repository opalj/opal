/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import scala.language.implicitConversions

import org.opalj.collection.immutable.Naught
import org.opalj.collection.immutable.Chain

/**
 * A collection of up to [[org.opalj.hermes.HermesConfig.MaxLocations]] locations where a specific
 * feature was found.
 *
 * Using a `LocationsContainer` has the advantage that we do not store unwanted locations.
 *
 * @tparam S The kind of the source. E.g., `java.net.URL`.
 *
 * @author Michael Eichberg
 */
class LocationsContainer[S](implicit hermes: HermesConfig) {

    private var theLocationsCount = 0
    private var theLocations: Chain[Location[S]] = Naught

    def +=(location: ⇒ Location[S]): Unit = {
        theLocationsCount += 1
        if (theLocationsCount <= hermes.MaxLocations) {
            theLocations :&:= location
        }
    }

    /** The number of locations that were seen. */
    def size: Int = theLocationsCount

    /**
     * The locations that were memorized; this depends on the global settings regarding the
     * precision and amount of location information that is kept.
     */
    def locations: Chain[Location[S]] = theLocations
}

object LocationsContainer {

    implicit def toLocationsChain[S](lc: LocationsContainer[S]): Chain[Location[S]] = {
        lc.locations
    }
}
