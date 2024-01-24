/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * Encapsulates the source of a method.
 */
case class MethodInfo[Source](source: Source, method: Method) {

    def classFile: ClassFile = method.classFile
}

/**
 * Defines a simplified extractor for a [[MethodInfo]] object.
 */
object BasicMethodInfo {

    def unapply(methodInfo: MethodInfo[_]): Some[Method] = Some(methodInfo.method)

}
