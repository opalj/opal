/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.hermes
package queries

import org.opalj.br.ObjectType
import org.opalj.collection.immutable.Chain
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

    override def apiFeatures: Chain[APIFeature] = {
        val Instrumentation = ObjectType("java/lang/instrument/Instrumentation")

        Chain(
            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Instrumentation, "retransformClasses"),
                    InstanceAPIMethod(Instrumentation, "addTransformer"),
                    InstanceAPIMethod(Instrumentation, "isModifiableClass"),
                    InstanceAPIMethod(Instrumentation, "isRetransformClassesSupported"),
                    InstanceAPIMethod(Instrumentation, "isRedefineClassesSupported"),
                    InstanceAPIMethod(Instrumentation, "redefineClasses")
                ), "class file retransformation"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Instrumentation, "setNativeMethodPrefix"),
                    InstanceAPIMethod(Instrumentation, "isNativeMethodPrefixSupported")
                ), "instrumenting native methods"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Instrumentation, "appendToBootstrapClassLoaderSearch"),
                    InstanceAPIMethod(Instrumentation, "appendToSystemClassLoaderSearch")
                ), "appending class loader search"
            ),

            APIFeatureGroup(
                Chain(
                    InstanceAPIMethod(Instrumentation, "getAllLoadedClasses"),
                    InstanceAPIMethod(Instrumentation, "getInitiatedClasses"),
                    InstanceAPIMethod(Instrumentation, "getObjectSize")
                ), "retrieve classes information"
            )
        )
    }
}
