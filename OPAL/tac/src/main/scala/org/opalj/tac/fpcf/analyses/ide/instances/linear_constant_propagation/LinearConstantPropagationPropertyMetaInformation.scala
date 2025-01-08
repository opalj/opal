/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation

import org.opalj.fpcf.PropertyKey
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationFact
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue
import org.opalj.tac.fpcf.analyses.ide.integration.JavaIDEPropertyMetaInformation

/**
 * Meta information for linear constant propagation
 */
object LinearConstantPropagationPropertyMetaInformation
    extends JavaIDEPropertyMetaInformation[LinearConstantPropagationFact, LinearConstantPropagationValue] {
    final val key: PropertyKey[Self] = PropertyKey.create("opalj.ide.LinearConstantPropagation")
}
