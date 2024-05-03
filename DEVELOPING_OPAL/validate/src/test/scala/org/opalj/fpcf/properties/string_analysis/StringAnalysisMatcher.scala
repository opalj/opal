/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package string_analysis

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.StringConstancyProperty

/**
 * @author Maximilian Rüsch
 */
class StringAnalysisMatcher extends AbstractPropertyMatcher {

    private def getLevelValue(a: AnnotationLike, valueName: String, optional: Boolean): StringConstancyLevel = {
        a.elementValuePairs.find(_.name == valueName) match {
            case Some(el)          => StringConstancyLevel.valueOf(el.value.asEnumValue.constName)
            case None if !optional => throw new IllegalArgumentException(s"Could not find $valueName in annotation $a")
            case None              => StringConstancyLevel.UNSPECIFIED
        }
    }

    private def getStringsValue(a: AnnotationLike, valueName: String, optional: Boolean): String = {
        a.elementValuePairs.find(_.name == valueName) match {
            case Some(el)          => el.value.asStringValue.value
            case None if !optional => throw new IllegalArgumentException(s"Could not find $valueName in annotation $a")
            case None              => StringDefinitions.NO_STRINGS
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
        properties: Iterable[Property]
    ): Option[String] = {
        if (
            a.annotationType.asObjectType != ObjectType("org/opalj/fpcf/properties/string_analysis/StringDefinitions")
        ) {
            throw new IllegalArgumentException(
                "Can only extract the constancy level from a @StringDefinitions annotation"
            )
        }

        val realisticLevel = getLevelValue(a, "realisticLevel", optional = true)
        val realisticStrings = getStringsValue(a, "realisticStrings", optional = true)
        val expectedLevel = getLevelValue(a, "expectedLevel", optional = false)
        val expectedStrings = getStringsValue(a, "expectedStrings", optional = false)

        if (realisticLevel == expectedLevel && realisticStrings == expectedStrings) {
            throw new IllegalStateException("Invalid test definition: Realistic and expected values are equal")
        } else if (
            realisticLevel == StringConstancyLevel.UNSPECIFIED ^ realisticStrings == StringDefinitions.NO_STRINGS
        ) {
            throw new IllegalStateException(
                "Invalid test definition: Realistic values must either be fully specified or be absent"
            )
        }

        val testRealisticValues =
            realisticLevel != StringConstancyLevel.UNSPECIFIED && realisticStrings != StringDefinitions.NO_STRINGS
        val (testedLevel, testedStrings) =
            if (testRealisticValues) (realisticLevel.toString.toLowerCase, realisticStrings)
            else (expectedLevel.toString.toLowerCase, expectedStrings)

        val (actLevel, actString) = properties.head match {
            case prop: StringConstancyProperty =>
                val sci = prop.stringConstancyInformation
                (sci.constancyLevel.toString.toLowerCase, sci.toRegex)
            case _ => ("", "")
        }

        if (testedLevel != actLevel || testedStrings != actString) {
            Some(s"Level: $testedLevel, Strings: $testedStrings")
        } else {
            None
        }
    }

}
