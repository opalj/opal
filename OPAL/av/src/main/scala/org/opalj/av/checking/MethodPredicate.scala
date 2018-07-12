/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import org.opalj.br.MethodDescriptor
import org.opalj.br.Method

/**
 * @author Marco Torsello
 */
case object AnyMethod extends SourceElementPredicate[Method] {

    final override def apply(method: Method): Boolean = true

    def toDescription(): String = "/*any method*/"

}

/**
 * @author Marco Torsello
 */
case class MethodWithName(name: String) extends SourceElementPredicate[Method] {

    def apply(method: Method): Boolean = {
        method.name == name
    }

    def toDescription(): String = name

}

/**
 * @author Marco Torsello
 */
case class MethodWithSignature(
        name:       String,
        descriptor: MethodDescriptor
) extends SourceElementPredicate[Method] {

    def apply(method: Method): Boolean = {
        method.name == this.name && method.descriptor == this.descriptor
    }

    def toDescription(): String = descriptor.toJava(name)

}

