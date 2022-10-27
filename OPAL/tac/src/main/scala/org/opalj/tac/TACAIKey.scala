/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br.Method
import org.opalj.br.analyses.JavaProjectInformationKey
import org.opalj.value.ValueInformation

/**
 * @author Michael Eichberg
 */
trait TACAIKey[I <: AnyRef]
    extends JavaProjectInformationKey[Method => AITACode[TACMethodParameter, ValueInformation], I]
