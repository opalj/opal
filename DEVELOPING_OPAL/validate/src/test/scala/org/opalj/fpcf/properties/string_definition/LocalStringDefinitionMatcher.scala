/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.fpcf.properties.string_definition

import org.opalj.br.analyses.Project
import org.opalj.br.AnnotationLike
import org.opalj.br.EnumValue
import org.opalj.br.ObjectType
import org.opalj.br.StringValue
import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.StringConstancyProperty

import scala.collection.mutable.ListBuffer

/**
 * Matches local variable's `StringConstancy` property. The match is successful if the
 * variable has a constancy level that matches its actual usage and the expected values are present.
 *
 * @author Patrick Mell
 */
class LocalStringDefinitionMatcher extends AbstractPropertyMatcher {

    /**
     * Returns the constancy levels specified in the annotation as a list of lower-cased strings.
     */
    private def getExpectedConstancyLevels(a: AnnotationLike): List[String] =
        a.elementValuePairs.find(_.name == "expectedLevels") match {
            case Some(el) ⇒
                el.value.asArrayValue.values.asInstanceOf[RefArray[EnumValue]].map {
                    ev: EnumValue ⇒ ev.constName.toLowerCase
                }.toList
            case None ⇒ List()
        }

    /**
     * Returns the expected strings specified in the annotation as a list.
     */
    private def getExpectedStrings(a: AnnotationLike): List[String] =
        a.elementValuePairs.find(_.name == "expectedStrings") match {
            case Some(el) ⇒
                el.value.asArrayValue.values.asInstanceOf[RefArray[StringValue]].map {
                    sc: StringValue ⇒ sc.value
                }.toList
            case None ⇒ List()
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
        val actLevels = ListBuffer[String]()
        val actStrings = ListBuffer[String]()
        if (properties.nonEmpty) {
            properties.head match {
                case prop: StringConstancyProperty ⇒
                    prop.stringConstancyInformation.foreach { nextSci ⇒
                        actLevels.append(nextSci.constancyLevel.toString.toLowerCase)
                        actStrings.append(nextSci.possibleStrings.toString)
                    }
                case _ ⇒
            }
        }

        val expLevels = getExpectedConstancyLevels(a)
        val expStrings = getExpectedStrings(a)
        val errorMsg = s"Levels: ${actLevels.mkString("{", ",", "}")}, "+
            s"Strings: ${actStrings.mkString("{", ",", "}")}"

        // The lists need to have the same sizes and need to match element-wise
        if (actLevels.size != expLevels.size || actStrings.size != expStrings.size) {
            return Some(errorMsg)
        }
        for (i ← actLevels.indices) {
            if (expLevels(i) != actLevels(i) || expStrings(i) != actStrings(i)) {
                return Some(errorMsg)
            }
        }

        None
    }

}
