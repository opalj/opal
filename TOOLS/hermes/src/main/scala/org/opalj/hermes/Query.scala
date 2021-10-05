/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes

import org.opalj.log.OPALLogger.error
import org.opalj.log.GlobalLogContext

/**
 * Container for feature queries.
 *
 * @note   Used to represent the corresponding information in the general configuration file.
 * @param  query The name of a concrete class which inherits from `FeatureQuery` and implements
 *         a default constructor.
 *
 * @author Michael Eichberg
 */
class Query(val query: String, private[this] var activate: Boolean = true) {

    def isEnabled: Boolean = activate

    private[this] var reifiedQuery: Option[FeatureQuery] = null

    def reify(implicit hermes: HermesConfig): Option[FeatureQuery] = this.synchronized {
        if (reifiedQuery ne null) {
            return reifiedQuery;
        }

        reifiedQuery =
            try {
                val queryClass = Class.forName(query, false, getClass.getClassLoader)
                val queryClassConstructor = queryClass.getDeclaredConstructor(classOf[HermesConfig])
                Some(queryClassConstructor.newInstance(hermes).asInstanceOf[FeatureQuery])
            } catch {
                case t: Throwable =>
                    error("application configuration", s"failed to load: $query", t)(GlobalLogContext)
                    activate = false
                    None
            }
        reifiedQuery
    }

}
