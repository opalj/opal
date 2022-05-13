/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.hermes
package queries

import org.opalj.br.ObjectType
import org.opalj.hermes.queries.util.APIFeature
import org.opalj.hermes.queries.util.APIFeatureGroup
import org.opalj.hermes.queries.util.APIFeatureQuery
import org.opalj.hermes.queries.util.InstanceAPIMethod

/**
 * Extracts basic information about the usage of the Java Instrumentation API that has been
 * enhanced in Java 6.
 *
 * @author Michael Reif
 */
class BytecodeInstrumentationAPIUsage(implicit hermes: HermesConfig) extends APIFeatureQuery {

    override def apiFeatures: List[APIFeature] = {
        val Instrumentation = ObjectType("java/lang/instrument/Instrumentation")

        List(
            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Instrumentation, "retransformClasses"),
                    InstanceAPIMethod(Instrumentation, "addTransformer"),
                    InstanceAPIMethod(Instrumentation, "isModifiableClass"),
                    InstanceAPIMethod(Instrumentation, "isRetransformClassesSupported"),
                    InstanceAPIMethod(Instrumentation, "isRedefineClassesSupported"),
                    InstanceAPIMethod(Instrumentation, "redefineClasses")
                ), "class file retransformation"
            ),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Instrumentation, "setNativeMethodPrefix"),
                    InstanceAPIMethod(Instrumentation, "isNativeMethodPrefixSupported")
                ), "instrumenting native methods"
            ),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Instrumentation, "appendToBootstrapClassLoaderSearch"),
                    InstanceAPIMethod(Instrumentation, "appendToSystemClassLoaderSearch")
                ), "appending class loader search"
            ),

            APIFeatureGroup(
                List(
                    InstanceAPIMethod(Instrumentation, "getAllLoadedClasses"),
                    InstanceAPIMethod(Instrumentation, "getInitiatedClasses"),
                    InstanceAPIMethod(Instrumentation, "getObjectSize")
                ), "retrieve classes information"
            )
        )
    }
}
