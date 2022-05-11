/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.br.BootstrapMethod
import org.opalj.br.FieldType

/**
 * Implements the handling of dynamic ldc/ldc_w/ldc2_w instructions at the type level.
 *
 * (Linkage related exceptions are currently generally ignored.)
 *
 * @author Dominik Helm
 */
trait TypeLevelDynamicLoads extends DynamicLoadsDomain {
    domain: ReferenceValuesDomain with TypedValuesFactory with Configuration =>

    /*override*/ def loadDynamic(
        pc:              Int,
        bootstrapMethod: BootstrapMethod,
        fieldName:       String,
        descriptor:      FieldType
    ): Computation[DomainValue, Nothing] = {
        doLoadDynamic(pc, TypedValue(pc, descriptor))
    }

    /*override*/ def doLoadDynamic(
        pc:            Int,
        constantValue: DomainValue
    ): Computation[DomainValue, Nothing] = {
        ComputedValue(constantValue)
    }

}
