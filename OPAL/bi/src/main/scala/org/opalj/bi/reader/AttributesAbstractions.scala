/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import scala.reflect.ClassTag

/**
 * Defines common abstractions over class file attributes.
 */
trait AttributesAbstractions {

    /*
     * Specifying a lower bound is necessary to implement a generic SkipAttributeMethod.
     */
    type Attribute >: Null
    implicit val AttributeManifest: ClassTag[Attribute]

    type Attributes = Seq[Attribute]

}
