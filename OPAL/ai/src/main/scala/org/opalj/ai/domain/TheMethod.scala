/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.Code

/**
 * Provides information about the method that is currently analyzed.
 *
 * ==Usage==
 * A domain that implements this trait usually defines a parameter that is set
 * at construction time.
 *
 * E.g.,
 * {{{
 * class MyDomain{val method : Method} extends Domain with TheMethod
 * }}}
 *
 * ==Core Properties==
 *  - Defines the public interface.
 *  - Makes the analyzed [[org.opalj.br.Method]] (and its [[org.opalj.br.Code]]) available.
 *  - Thread safe.
 *
 * @author Michael Eichberg
 */
trait TheMethod extends TheCode {

    /**
     * Returns the method that is currently analyzed.
     */
    def method: Method

    @inline final def classFile: ClassFile = method.classFile

    /**
     * Returns the code block that is currently analyzed.
     */
    final /*override*/ val code: Code = method.body.get

    override def toString(): String = {
        super.toString + s" with TheMethod(${method.toJava})"
    }

}
