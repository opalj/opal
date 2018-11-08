/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_definition

import org.opalj.br.analyses.Project
import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.StringConstancyProperty

/**
 * Matches local variable's `StringConstancy` property. The match is successful if the
 * variable has a constancy level that matches its actual usage and the expected values are present.
 *
 * @author Patrick Mell
 */
class LocalStringDefinitionMatcher extends AbstractPropertyMatcher {

    /**
     * @param a An annotation like of type
     *          [[org.opalj.fpcf.properties.string_definition.StringDefinitions]].
     * @return Returns the constancy level specified in the annotation as a string and `None` in
     *         case the element with the name 'expectedLevel' was not present in the annotation
     *         (should never be the case if an annotation of the correct type is passed).
     */
    private def getConstancyLevel(a: AnnotationLike): Option[String] = {
        a.elementValuePairs.find(_.name == "expectedLevel") match {
            case Some(el) ⇒ Some(el.value.asEnumValue.constName)
            case None     ⇒ None
        }
    }

    /**
     * @param a An annotation like of type
     *          [[org.opalj.fpcf.properties.string_definition.StringDefinitions]].
     * @return Returns the ''expectedStrings'' value from the annotation or `None` in case the
     *         element with the name ''expectedStrings'' was not present in the annotation (should
     *         never be the case if an annotation of the correct type is passed).
     */
    private def getExpectedStrings(a: AnnotationLike): Option[String] = {
        a.elementValuePairs.find(_.name == "expectedStrings") match {
            case Some(el) ⇒ Some(el.value.asStringValue.value)
            case None     ⇒ None
        }
    }

    /**
     * Takes an [[AnnotationLike]] which represents a [[org.opalj.fpcf.properties.StringConstancyProperty]] and returns its
     * stringified representation.
     *
     * @param a The annotation. This function requires that it holds a StringConstancyProperty.
     * @return The stringified representation, which is identical to
     *         [[org.opalj.fpcf.properties.StringConstancyProperty.toString]].
     */
    private def propertyAnnotation2Str(a: AnnotationLike): String = {
        val constancyLevel = getConstancyLevel(a).get.toLowerCase
        val ps = getExpectedStrings(a).get
        s"StringConstancyProperty { Constancy Level: $constancyLevel; Possible Strings: $ps }"
    }

    /**
     * @inheritdoc
     */
    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        val prop = properties.filter(
            _.isInstanceOf[StringConstancyProperty]
        ).head.asInstanceOf[StringConstancyProperty]
        val reducedProp = prop.stringTree.reduce()

        val expLevel = getConstancyLevel(a).get
        val actLevel = reducedProp.constancyLevel.toString
        if (expLevel.toLowerCase != actLevel.toLowerCase) {
            return Some(propertyAnnotation2Str(a))
        }

        // TODO: This string comparison is not very robust
        val expStrings = getExpectedStrings(a).get
        if (expStrings != reducedProp.possibleStrings) {
            return Some(propertyAnnotation2Str(a))
        }

        None
    }

}
