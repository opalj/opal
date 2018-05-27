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

import org.opalj.collection.immutable.Naught
import org.opalj.collection.immutable.Chain

/**
 * Represents the immutable results of a feature query.
 *
 * @param  id A very short identifier of this feature. E.g., `Java8ClassFile` or
 *         `ProtectedMethod` or `DeadMethod`. The name must not contain spaces or other
 *         special characters.
 * @param  count How often the feature was found in a project.
 * @param  extensions Places where the feature was found. This information is
 *         primarily useful when exploring the project and is optional.
 *         I.e., `extensions.size` can be  smaller than `count`. The maximum number
 *         of stored locations is set using the global setting: `org.opalj.hermes.maxLocations`.
 *
 * @author Michael Eichberg
 */
abstract case class Feature[S] private (
        id:         String,
        count:      Int,
        extensions: Chain[Location[S]]
) {
    assert(count >= extensions.size)
}

/**
 * Factory to create features.
 *
 * @author Michael Eichberg
 */
object Feature {

    def apply[S](
        id:         String,
        count:      Int                = 0,
        extensions: Chain[Location[S]] = Naught
    )(
        implicit
        hermes: HermesConfig
    ): Feature[S] = {
        new Feature(id, count, extensions.takeUpTo(hermes.MaxLocations)) {}
    }

    def apply[S](
        id:        String,
        locations: LocationsContainer[S]
    ): Feature[S] = {
        new Feature(id, locations.size, locations.locations) {}
    }
}
