/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_definition

import org.opalj.br.analyses.Project
import org.opalj.br.AnnotationLike
import org.opalj.br.ElementValue
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
     * @return Returns an array of strings with the expected / possible string values and `None` in
     *         case the element with the name 'expectedValues' was not present in the annotation
     *         (should never be the case if an annotation of the correct type is passed).
     */
    private def getPossibleStrings(a: AnnotationLike): Option[Array[String]] = {
        a.elementValuePairs.find(_.name == "expectedValues") match {
            case Some(el) ⇒ Some(
                el.value.asArrayValue.values.map { f: ElementValue ⇒ f.asStringValue.value }.toArray
            )
            case None ⇒ None
        }
    }

    private def aToMsg(a: AnnotationLike): String = {
        val constancyLevel = getConstancyLevel(a).get.toLowerCase
        val ps = getPossibleStrings(a).get.mkString("[", ", ", "]")
        s"StringConstancyProperty { Constancy Level: $constancyLevel; Possible Strings: $ps }"
    }

    /**
     * @param a1 The first array.
     * @param a2 The second array.
     * @return Returns true if both arrays have the same length and all values of the first array
     *         are contained in the second array.
     */
    private def doArraysContainTheSameValues(a1: Array[String], a2: Array[String]): Boolean = {
        if (a1.length != a2.length) {
            return false
        }
        a1.map(a2.contains(_)).forall { b ⇒ b }
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

        val expLevel = getConstancyLevel(a).get
        val actLevel = prop.constancyLevel.toString
        if (expLevel.toLowerCase != actLevel.toLowerCase) {
            return Some(aToMsg(a))
        }

        val expStrings = prop.possibleStrings.toArray
        val actStrings = getPossibleStrings(a).get
        if (!doArraysContainTheSameValues(expStrings, actStrings)) {
            return Some(aToMsg(a))
        }

        None
    }

}
