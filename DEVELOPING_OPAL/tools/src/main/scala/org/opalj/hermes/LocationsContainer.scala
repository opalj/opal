/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
