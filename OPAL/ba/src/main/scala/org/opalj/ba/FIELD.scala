/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

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
        attributesBuilders: Seq[br.FieldAttributeBuilder] = Seq.empty
) {

    /**
     * Returns the build [[org.opalj.br.Method]] and its annotations.
     */
    def result(): br.FieldTemplate = {
        val fieldType = br.FieldType(descriptor)
        val accessFlags = accessModifiers.accessFlags
        val attributes = attributesBuilders map { attributeBuilder ⇒
            attributeBuilder(accessFlags, name, fieldType)
        }

        br.Field(accessFlags, name, fieldType, attributes)
    }
}
