/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

/**
 * Common interface of all entries in the constant pool.
 */
trait ConstantPoolEntry {

    def asString: String

}
