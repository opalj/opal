/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import org.opalj.br.BootstrapMethod
import org.opalj.br.FieldType

/**
 * Interface related to the handling of dynamic ldc/ldc_w/ldc2_w instructions.
 *
 * @author Dominik Helm
 */
trait DynamicLoadsDomain { this: ValuesDomain =>

    /**
     * Returns the dynamic constant's value.
     */
    def loadDynamic(
        pc:              Int,
        bootstrapMethod: BootstrapMethod,
        name:            String,
        descriptor:      FieldType
    ): Computation[DomainValue, Nothing]

}
