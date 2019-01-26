/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_analysis

import org.opalj.br.analyses.Project
import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.fpcf.Property
import org.opalj.br.fpcf.properties.StringConstancyProperty

/**
 * Matches local variable's `StringConstancy` property. The match is successful if the
 * variable has a constancy level that matches its actual usage and the expected values are present.
 *
 * @author Patrick Mell
 */
class StringAnalysisMatcher extends AbstractPropertyMatcher {

    /**
     * @param a An annotation like of type
     *          [[org.opalj.fpcf.properties.string_analysis.StringDefinitions]].
     *
     * @return Returns the constancy level specified in the annotation as a string. In case an
     *         annotation other than StringDefinitions is passed, an [[IllegalArgumentException]]
     *         will be thrown (since it cannot be processed).
     */
    private def getConstancyLevel(a: AnnotationLike): String = {
        a.elementValuePairs.find(_.name == "expectedLevel") match {
            case Some(el) ⇒ el.value.asEnumValue.constName
            case None ⇒ throw new IllegalArgumentException(
                "Can only extract the constancy level from a StringDefinitions annotation"
            )
        }
    }

    /**
     * @param a An annotation like of type
     *          [[org.opalj.fpcf.properties.string_analysis.StringDefinitions]].
     *
     * @return Returns the ''expectedStrings'' value from the annotation. In case an annotation
     *         other than StringDefinitions is passed, an [[IllegalArgumentException]] will be
     *         thrown (since it cannot be processed).
     */
    private def getExpectedStrings(a: AnnotationLike): String = {
        a.elementValuePairs.find(_.name == "expectedStrings") match {
            case Some(el) ⇒ el.value.asStringValue.value
            case None ⇒ throw new IllegalArgumentException(
                "Can only extract the possible strings from a StringDefinitions annotation"
            )
        }
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
        var actLevel = ""
        var actString = ""
        properties.head match {
            case prop: StringConstancyProperty ⇒
                val sci = prop.stringConstancyInformation
                actLevel = sci.constancyLevel.toString.toLowerCase
                actString = sci.possibleStrings
            case _ ⇒
        }

        val expLevel = getConstancyLevel(a).toLowerCase
        val expStrings = getExpectedStrings(a)
        val errorMsg = s"Level: $expLevel, Strings: $expStrings"

        if (expLevel != actLevel || expStrings != actString) {
            return Some(errorMsg)
        }

        None
    }

}
