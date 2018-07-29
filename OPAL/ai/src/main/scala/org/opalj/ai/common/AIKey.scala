/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package common

import org.opalj.br.Method
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.ai.domain.RecordDefUse

/**
 * ''Key'' to get the result of the abstract interpretation of a method.
 *
 * @author Michael Eichberg
 */
trait AIKey
    extends ProjectInformationKey[Method ⇒ AIResult { val domain: Domain with RecordDefUse }, /*DomainFactory*/ Method ⇒ Domain with RecordDefUse]
