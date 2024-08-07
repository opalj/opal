/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields

import org.opalj.fpcf.PropertyKey
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.LCPOnFieldsFact
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.LCPOnFieldsValue

/**
 * Meta information for linear constant propagation on fields
 */
object LCPOnFieldsPropertyMetaInformation
    extends IDEPropertyMetaInformation[LCPOnFieldsFact, LCPOnFieldsValue] {
    final val key: PropertyKey[Self] = PropertyKey.create("opalj.ide.LinearConstantPropagationOnFields")
}
