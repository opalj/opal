/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import scala.reflect.ClassTag

import org.opalj.bi.reader.AttributesAbstractions

/**
 * Defines the common bindings for all "resolved" attributes.
 *
 * @author Michael Eichberg
 */
trait AttributeBinding extends AttributesAbstractions {

    type Attribute = br.Attribute
    override implicit val attributeType: ClassTag[Attribute] = ClassTag(classOf[br.Attribute])
}
