/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds

import org.opalj.br.Method
import org.opalj.ifds.Callable

/**
 * Representation of a Java Method for the IFDS Analysis
 * @param method the represented method
 *
 * @author Marc Clement
 */
case class JavaMethod(method: Method) extends Callable {
    override def name: String      = method.name
    override def signature: String = method.toJava
}
