/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.hermes
package queries

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
    override val apiFeatures: List[APIFeature] = {

        val Thread = ObjectType("java/lang/Thread")
        val ThreadGroup = ObjectType("java/lang/ThreadGroup")
        val Object = ObjectType("java/lang/Object")

        val constructor = "<init>"

        List(

            // PROCESS

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Object, "notify"),
                    InstanceAPIMethod(Object, "notifyAll"),
                    InstanceAPIMethod(Object, "wait")
                ),
                "Object-based Thread Notification"
            ),

            APIFeatureGroup(
                List(
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
                List(
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
