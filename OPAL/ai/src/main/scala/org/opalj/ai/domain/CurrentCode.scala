/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import scala.compiletime.uninitialized

import org.opalj.br.Code
import org.opalj.collection.immutable.IntTrieSet

/**
 * Provides information about the code block that is currently analyzed.
 *
 * ==Core Properties==
 *  - "Automatically reset" when the domain is used to analyze another method.
 *  - "Concurrent Usage": "No"
 *
 * @author Michael Eichberg
 */
trait CurrentCode extends TheCode with CustomInitialization { domain: ValuesDomain =>

    private var theCode: Code = uninitialized

    /**
     * Returns the code block that is currently analyzed.
     */
    final def code: Code = theCode

    abstract override def initProperties(
        code:          Code,
        cfJoins:       IntTrieSet,
        initialLocals: Locals
    ): Unit = {

        this.theCode = code

        super.initProperties(code, cfJoins, initialLocals)
    }

}
