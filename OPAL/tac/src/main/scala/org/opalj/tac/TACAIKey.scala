/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.value.ValueInformation

/**
 * @author Michael Eichberg
 */
trait TACAIKey[I <: AnyRef]
    extends ProjectInformationKey[Method ⇒ AITACode[TACMethodParameter, ValueInformation], I]
