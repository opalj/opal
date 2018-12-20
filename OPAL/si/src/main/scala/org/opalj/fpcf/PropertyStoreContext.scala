/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

/**
 * Object stored in the `PropertyStore` to provide general context information.
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
