/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

class PropertyStoreContext[+T <: AnyRef] private (val key: Class[_], val data: T) {

    def asTuple: (Class[_], T) = (key, data)
}

object PropertyStoreContext {

    def apply[T <: AnyRef](key: Class[T], data: T): PropertyStoreContext[T] = {
        new PropertyStoreContext(key, data)
    }

}
