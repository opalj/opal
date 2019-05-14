/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

/**
 * Object stored in the `PropertyStore` to provide general context information.
 *
 * Two typical objects that are stored by OPAL's subprojects in the context are:
 *  - the project (`org.opalj.br.analysis.Project`)
 *  - the (project dependent) configuration (`com.typesafe.config.Config`)
 *
 * @author Michael Eichberg
 */
class PropertyStoreContext[+T <: AnyRef] private (val key: Class[_], val data: T) {
    // FIXME Make the context invariant and then use the tParam T in "asTuple"
    def asTuple: (Class[_], T) = (key, data)
}

object PropertyStoreContext {

    def apply[T <: AnyRef](key: Class[T], data: T): PropertyStoreContext[T] = {
        new PropertyStoreContext(key, data)
    }

}
