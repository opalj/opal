/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.value.KnownTypedValue

/**
 * @author Michael Eichberg
 */
trait TACAIKey
    extends ProjectInformationKey[Method ⇒ TACode[TACMethodParameter, DUVar[KnownTypedValue]], Nothing]
