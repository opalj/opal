/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * We treat as a source element every entity that can be referred to
 * by other class files.
 *
 * @author Michael Eichberg
 */
trait SourceElement extends CommonSourceElementAttributes {

    def isClass = false
    def isMethod = false
    def isField = false

    def isVirtual = false

    def asClassFile: ClassFile = throw new UnsupportedOperationException
    def asMethod: Method = throw new UnsupportedOperationException
    def asField: Field = throw new UnsupportedOperationException

}
