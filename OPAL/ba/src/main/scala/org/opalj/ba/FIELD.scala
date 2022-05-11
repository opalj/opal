/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import scala.collection.immutable.ArraySeq

/**
 * Builder for a [[org.opalj.br.Field]]; a `FIELD` object is intended to be stored in a
 * [[org.opalj.ba.FIELDS]] collection.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
case class FIELD(
        accessModifiers:    AccessModifier,
        name:               String,
        descriptor:         String,
        attributesBuilders: ArraySeq[br.FieldAttributeBuilder] = ArraySeq.empty
) {

    /**
     * Returns the build [[org.opalj.br.Method]] and its annotations.
     */
    def result(): br.FieldTemplate = {
        val fieldType = br.FieldType(descriptor)
        val accessFlags = accessModifiers.accessFlags
        val attributes = attributesBuilders.map[br.Attribute] { attributeBuilder =>
            attributeBuilder(accessFlags, name, fieldType)
        }

        br.Field(accessFlags, name, fieldType, attributes)
    }
}
