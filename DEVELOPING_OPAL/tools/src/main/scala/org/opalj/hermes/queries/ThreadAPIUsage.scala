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
package org.opalj.hermes
package queries

import org.opalj.collection.immutable.Chain
import org.opalj.br.ObjectType
import org.opalj.hermes.queries.util.APIFeatureGroup
import org.opalj.hermes.queries.util.APIFeatureQuery
import org.opalj.hermes.queries.util.APIFeature
import org.opalj.hermes.queries.util.InstanceAPIMethod
import org.opalj.hermes.queries.util.StaticAPIMethod

/**
 * Captures the usage of Thread-related API usage.
 *
 * @author Ben Hermann
 */
class ThreadAPIUsage(implicit hermes: HermesConfig) extends APIFeatureQuery {
    override val apiFeatures: Chain[APIFeature] = {

        val Thread = ObjectType("java/lang/Thread")
        val ThreadGroup = ObjectType("java/lang/ThreadGroup")
        val Object = ObjectType("java/lang/Object")

        val constructor = "<init>"

        Chain(

            // PROCESS

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Object, "notify"),
                    InstanceAPIMethod(Object, "notifyAll"),
                    InstanceAPIMethod(Object, "wait")
                ),
                "Object-based Thread Notification"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Thread, constructor),
                    InstanceAPIMethod(Thread, "interrupt"),
                    InstanceAPIMethod(Thread, "join"),
                    InstanceAPIMethod(Thread, "run"),
                    StaticAPIMethod(Thread, "sleep"),
                    InstanceAPIMethod(Thread, "start"),
                    // Deprecated members
                    InstanceAPIMethod(Thread, "destroy"),
                    InstanceAPIMethod(Thread, "resume"),
                    InstanceAPIMethod(Thread, "stop"),
                    InstanceAPIMethod(Thread, "suspend")
                ),
                "Usage of Thread API"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(ThreadGroup, constructor),
                    InstanceAPIMethod(ThreadGroup, "destroy"),
                    InstanceAPIMethod(ThreadGroup, "interrupt"),
                    // Deprecated members
                    InstanceAPIMethod(ThreadGroup, "resume"),
                    InstanceAPIMethod(ThreadGroup, "stop"),
                    InstanceAPIMethod(ThreadGroup, "suspend")
                ),
                "Usage of ThreadGroup API"
            )

        )
    }

}
