/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.Code
import org.opalj.collection.immutable.IntTrieSet

/**
 * Mixin this trait if a domain needs to perform some custom initialization.
 *
 * ==Usage==
 * It is sufficient to mixin this trait in a [[Domain]] that needs custom initialization.
 * The abstract interpreter will then perform the initialization.
 *
 * This information is set immediately before the abstract interpretation is
 * started/continued. I.e., this makes it potentially possible to reuse a [[Domain]] object
 * for the interpretation of multiple methods.
 *
 * @author Michael Eichberg
 */
trait CustomInitialization { domain: ValuesDomain =>

    /**
     * Override this method to perform custom initialization steps.
     *
     * Always use `abstract override` and call the super method; it is recommended
     * to complete the initialization of this domain before calling the super method.
     */
    def initProperties(code: Code, cfJoins: IntTrieSet, initialLocals: Locals): Unit = {
        // Empty by default.
    }

}
