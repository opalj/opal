/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation

import org.opalj.fpcf.PropertyKey
import org.opalj.ide.integration.IDEPropertyMetaInformation
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationFact
import org.opalj.tac.fpcf.analyses.ide.instances.linear_constant_propagation.problem.LinearConstantPropagationValue

/**
 * Meta information for linear constant propagation
 */
object LinearConstantPropagationPropertyMetaInformation
    extends IDEPropertyMetaInformation[LinearConstantPropagationFact, LinearConstantPropagationValue] {
    final val key: PropertyKey[Self] = PropertyKey.create("IDELinearConstantPropagation")
}
