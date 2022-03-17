/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds

import org.opalj.br.Method

abstract class Callable {
    /**
     * The name of the Callable
     */
    def name(): String

    /**
     * The full name of the Callable including its signature
     */
    def signature(): String
}

case class JavaMethod(val method: Method) extends Callable {
    override def name(): String = method.name
    override def signature(): String = method.toJava
}
