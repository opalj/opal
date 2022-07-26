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
 * Queries the usage of the java.lang.invoke API around `MethodHandle`s.
 *
 * @note The features represent the __MODERN REFLECTION__ test cases from the Call Graph Test
 *       Project (JCG).
 *
 * @author Dominik Helm
 */
class ModernReflection(implicit hermes: HermesConfig) extends APIFeatureQuery {

    val Lookup = ObjectType.MethodHandles$Lookup
    val MethodHandle = ObjectType.MethodHandle

    override val apiFeatures: List[APIFeature] = {

        List(
            InstanceAPIMethod(
                Lookup, "findStatic", featureID = "TMR1.1"
            ),
            InstanceAPIMethod(
                MethodHandle, "invokeExact", featureID = "TMR1.2"
            ),
            InstanceAPIMethod(
                Lookup, "findVirtual", featureID = "TMR2"
            ),
            InstanceAPIMethod(
                Lookup, "findConstructor", featureID = "TMR3"
            ),
            InstanceAPIMethod(
                Lookup, "findStaticGetter", featureID = "TMR4"
            ),
            InstanceAPIMethod(
                Lookup, "findGetter", featureID = "TMR5"
            ),
            InstanceAPIMethod(
                Lookup, "findSpecial", featureID = "TMR6"
            ),
            InstanceAPIMethod(
                MethodHandle, "invoke", featureID = "TMR7"
            ),
            InstanceAPIMethod(
                MethodHandle, "invokeWithArguments", featureID = "TMR8"
            )
        )
    }
}
