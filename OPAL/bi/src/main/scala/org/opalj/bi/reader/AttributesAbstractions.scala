/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import org.opalj.collection.immutable.RefArray

/**
 * Defines common abstractions over class file attributes.
 */
trait AttributesAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    /** Specifying a lower bound is necessary to implement a generic `skipAttribute` method. */
    type Attribute >: Null <: AnyRef

    type Attributes = RefArray[Attribute]

}
