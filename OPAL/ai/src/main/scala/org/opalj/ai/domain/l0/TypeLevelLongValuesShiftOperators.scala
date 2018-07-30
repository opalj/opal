/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l0

import org.opalj.ai.LongValuesDomain

/**
 * This partial `Domain` performs all computations related to primitive long
 * values at the type level.
 *
 * This domain can be used as a foundation for building more complex domains.
 *
 * @author Michael Eichberg
 */
trait TypeLevelLongValuesShiftOperators extends LongValuesDomain {

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    /*override*/ def lshl(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        LongValue(pc)
    }

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    /*override*/ def lshr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        LongValue(pc)
    }

    /**
     * @inheritdoc
     *
     * @return The result of calling `LongValue(pc)`.
     */
    /*override*/ def lushr(pc: Int, value1: DomainValue, value2: DomainValue): DomainValue = {
        LongValue(pc)
    }

}
