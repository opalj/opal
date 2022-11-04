/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import org.opalj.br.ObjectType
import org.opalj.hermes.queries.util.APIFeature
import org.opalj.hermes.queries.util.APIFeatureQuery
import org.opalj.hermes.queries.util.StaticAPIMethod

/**
 * Queries the usage of the proxy api (java.lang.reflect.Proxy).
 *
 * @author Florian Kuebler
 */
class DynamicProxy(implicit hermes: HermesConfig) extends APIFeatureQuery {

    override val apiFeatures: List[APIFeature] = {

        List(
            StaticAPIMethod(
                ObjectType("java/lang/reflect/Proxy"), "newProxyInstance", featureID = "DP1"
            )
        )
    }
}