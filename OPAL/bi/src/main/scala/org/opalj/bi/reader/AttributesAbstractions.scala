/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import scala.reflect.ClassTag

import scala.collection.immutable.ArraySeq

/**
 * Defines common abstractions over class file attributes.
 */
trait AttributesAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    /** Specifying a lower bound is necessary to implement a generic `skipAttribute` method. */
    type Attribute >: Null <: AnyRef: ClassTag

    type Attributes = ArraySeq[Attribute]

}
