/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package string_analysis

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.fpcf.properties.string.StringConstancyLevel
import org.opalj.br.fpcf.properties.string.StringConstancyProperty

/**
 * @author Maximilian Rüsch
 */
sealed trait StringMatcher extends AbstractPropertyMatcher {

    protected def getActualValues: Property => Option[(String, String)] = {
        case prop: StringConstancyProperty =>
            val tree = prop.sci.tree.simplify.sorted
            if (tree.isInvalid) {
                None
            } else {
                Some((tree.constancyLevel.toString.toLowerCase, tree.toRegex))
            }
        case p => throw new IllegalArgumentException(s"Tried to extract values from non string property: $p")
    }
}

sealed abstract class ConstancyStringMatcher(val constancyLevel: StringConstancyLevel.Value) extends StringMatcher {

    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val expectedConstancy = constancyLevel.toString.toLowerCase
        val expectedStrings = a.elementValuePairs.find(_.name == "value").get.value.asStringValue.value
        val actualValuesOpt = getActualValues(properties.head)
        if (actualValuesOpt.isEmpty) {
            Some(s"Level: $expectedConstancy, Strings: $expectedStrings")
        } else {
            val (actLevel, actString) = actualValuesOpt.get
            if (expectedConstancy != actLevel || expectedStrings != actString) {
                Some(s"Level: $expectedConstancy, Strings: $expectedStrings")
            } else {
                None
            }
        }
    }
}

class ConstantStringMatcher extends ConstancyStringMatcher(StringConstancyLevel.CONSTANT)
class PartiallyConstantStringMatcher extends ConstancyStringMatcher(StringConstancyLevel.PARTIALLY_CONSTANT)
class DynamicStringMatcher extends ConstancyStringMatcher(StringConstancyLevel.DYNAMIC)

class InvalidStringMatcher extends StringMatcher {

    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val actualValuesOpt = getActualValues(properties.head)
        if (actualValuesOpt.isDefined) {
            Some(s"Invalid flow - No strings determined!")
        } else {
            None
        }
    }
}
