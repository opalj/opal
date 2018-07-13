/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses.cg

import br.Method
import br.MethodDescriptor
import br.ReferenceType

/**
 * Represents a method call that could not be resolved; that is, the target of
 * an invoke instruction could not be found. This information is primarily
 * interesting during the development of static analyses.
 *
 * @author Michael Eichberg
 */
case class UnresolvedMethodCall(
        caller:           Method,
        pc:               PC,
        calleeClass:      ReferenceType,
        calleeName:       String,
        calleeDescriptor: MethodDescriptor
) {

    import Console._

    override def toString: String = {
        val target = s"${calleeClass.toJava}{{$BOLD${calleeDescriptor.toJava(calleeName)}$RESET}}"
        caller.toJava(s"pc=$pc: $target")
    }
}
