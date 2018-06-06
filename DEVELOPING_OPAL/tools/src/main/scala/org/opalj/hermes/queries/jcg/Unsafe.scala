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
package queries
package jcg

import org.opalj.br.ObjectType
import org.opalj.collection.immutable.Chain
import org.opalj.hermes.queries.util.APIFeature
import org.opalj.hermes.queries.util.APIFeatureQuery
import org.opalj.hermes.queries.util.InstanceAPIMethod

/**
 * Groups features that rely on the Unsafe API. (sun.misc.Unsafe)
 *
 * @note Feature groups are taken from and are further discussed in the following paper:
 *       "Use at Your Own Risk: The Java Unsafe API in the Wild"
 *       by Luis Mastrangelo et al.
 *
 * @author Michael Reif
 */
class Unsafe(implicit hermes: HermesConfig) extends APIFeatureQuery {

    override val apiFeatures: Chain[APIFeature] = {

        val Unsafe = ObjectType("sun/misc/Unsafe")

        Chain(
            InstanceAPIMethod(Unsafe, "compareAndSwapObject", featureID = "Unsafe1"),
            InstanceAPIMethod(Unsafe, "putObject", featureID = "Unsafe2"),
            InstanceAPIMethod(Unsafe, "getObject", featureID = "Unsafe3"),
            InstanceAPIMethod(Unsafe, "getAndSetObject", featureID = "Unsafe4"),
            InstanceAPIMethod(Unsafe, "putOrderedObject", featureID = "Unsafe5"),
            InstanceAPIMethod(Unsafe, "getObjectVolatile", featureID = "Unsafe6"),
            InstanceAPIMethod(Unsafe, "putObjectVolatile", featureID = "Unsafe7")
        )
    }
}
