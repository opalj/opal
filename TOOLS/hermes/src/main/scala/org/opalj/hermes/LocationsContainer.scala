/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import scala.language.implicitConversions

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
    private var theLocations: List[Location[S]] = List()

    def +=(location: => Location[S]): Unit = {
        theLocationsCount += 1
        if (theLocationsCount <= hermes.MaxLocations) {
            theLocations = location :: theLocations
        }
    }

    /** The number of locations that were seen. */
    def size: Int = theLocationsCount

    /**
     * The locations that were memorized; this depends on the global settings regarding the
     * precision and amount of location information that is kept.
     */
    def locations: List[Location[S]] = theLocations
}

object LocationsContainer {

    implicit def toLocationsChain[S](lc: LocationsContainer[S]): List[Location[S]] = {
        lc.locations
    }
}
