/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.Method
import org.opalj.ifds.Callable

/**
 * Rep
 * @param method
 */
case class JavaMethod(method: Method) extends Callable {
    override def name: String = method.name
    override def signature: String = method.toJava
}
