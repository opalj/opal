/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package field_locality

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.FieldLocality

/**
 * A property matcher that checks whether the annotated method has the specified field locality
 * property.
 *
 * @author Florian Kuebler
 */
class FieldLocalityMatcher(val property: FieldLocality) extends AbstractPropertyMatcher {
    /**
     * Tests if the computed property is matched by this matcher.
     *
     * @param p          The project.
     * @param as         The OPAL `ObjectType`'s of the executed analyses.
     * @param entity     The annotated entity.
     * @param a          The annotation.
     * @param properties '''All''' properties associated with the given entity.
     * @return 'None' if the property was successfully matched; 'Some(<String>)' if the
     *         property was not successfully matched; the String describes the reason
     *         why the analysis failed.
     */
    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists {
            case `property` => true
            case _          => false
        }) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

class NoLocalFieldMatcher
    extends FieldLocalityMatcher(br.fpcf.properties.NoLocalField)

class ExtensibleLocalFieldMatcher
    extends FieldLocalityMatcher(br.fpcf.properties.ExtensibleLocalField)

class LocalFieldMatcher
    extends FieldLocalityMatcher(br.fpcf.properties.LocalField)

class ExtensibleLocalFieldWithGetterMatcher
    extends FieldLocalityMatcher(br.fpcf.properties.ExtensibleLocalFieldWithGetter)

class LocalFieldWithGetterMatcher
    extends FieldLocalityMatcher(br.fpcf.properties.LocalFieldWithGetter)
