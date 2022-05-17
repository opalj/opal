/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.AttributesAbstractions

import scala.reflect.{ClassTag, classTag}

/**
 * Defines the common bindings for all "resolved" attributes.
 *
 * @author Michael Eichberg
 */
trait AttributeBinding extends AttributesAbstractions {

    type Attribute = org.opalj.br.Attribute
    override implicit val attributeType: ClassTag[Attribute] = classTag[Attribute]
}

