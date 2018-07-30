/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.opalj.br.Code

/**
 * Provides information about the code block that is currently analyzed.
 *
 * ==Core Properties==
 *  - Defines the public interface.
 *
 * @author Michael Eichberg
 */
trait TheCode {

    /**
     * Returns the code block that is currently analyzed.
     */
    def code: Code

}
