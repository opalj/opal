/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import org.opalj.br.ObjectType
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

    override val apiFeatures: List[APIFeature] = {

        val Unsafe = ObjectType("sun/misc/Unsafe")

        List(
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
