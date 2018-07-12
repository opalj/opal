/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.opalj.ai.Domain
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKey

/**
 * @author Michael Eichberg
 */
trait TACAIKey
    extends ProjectInformationKey[Method ⇒ TACode[TACMethodParameter, DUVar[(Domain with RecordDefUse)#DomainValue]], Nothing]
