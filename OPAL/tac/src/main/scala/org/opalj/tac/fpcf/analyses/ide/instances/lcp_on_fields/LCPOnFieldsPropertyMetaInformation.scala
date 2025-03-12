/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ide
package instances
package lcp_on_fields

import org.opalj.fpcf.PropertyKey
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.LCPOnFieldsFact
import org.opalj.tac.fpcf.analyses.ide.instances.lcp_on_fields.problem.LCPOnFieldsValue
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEPropertyMetaInformation

/**
 * Meta information for linear constant propagation on fields
 */
object LCPOnFieldsPropertyMetaInformation
    extends JavaIDEPropertyMetaInformation[LCPOnFieldsFact, LCPOnFieldsValue] {
    final val key: PropertyKey[Self] = PropertyKey.create("opalj.ide.LinearConstantPropagationOnFields")
}
