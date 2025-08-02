/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package ide

import scala.collection.immutable.ArraySeq

import org.opalj.br.AnnotationLike
import org.opalj.br.ClassType
import org.opalj.br.analyses.Project
import org.opalj.fpcf.Property
import org.opalj.fpcf.properties.AbstractPropertyMatcher
import org.opalj.ide.integration.BasicIDEProperty

/**
 * An [[AbstractPropertyMatcher]] with additional functions useful to write property matchers for IDE analyses.
 *
 * @author Robin Körkemeier
 */
trait IDEPropertyMatcher extends AbstractPropertyMatcher {
    /**
     * The annotation type that is processed
     */
    val singleAnnotationType: ClassType

    /**
     * Extract an element from an annotation, treat it as an array and apply a map operator to it.
     *
     * @param elementName the name of the array element
     */
    def mapArrayValue[V](
        p:           Project[?],
        a:           AnnotationLike,
        elementName: String,
        f:           AnnotationLike => V
    ): ArraySeq[V] = {
        getValue(p, singleAnnotationType, a.elementValuePairs, elementName).asArrayValue.values
            .map { a => f(a.asAnnotationValue.annotation) }
    }

    /**
     * Extract a string value from the objects of an array of an annotation.
     *
     * @param elementName the name of the array element
     * @param aInner the type of the objects inside the array
     * @param innerElementName the element to extract from the objects of the array
     */
    def mapArrayValueExtractString(
        p:                Project[?],
        a:                AnnotationLike,
        elementName:      String,
        aInner:           ClassType,
        innerElementName: String
    ): ArraySeq[String] = {
        mapArrayValue(
            p,
            a,
            elementName,
            { annotation => getValue(p, aInner, annotation.elementValuePairs, innerElementName).asStringValue.value }
        )
    }

    /**
     * Extract an int value from the objects of an array of an annotation.
     *
     * @param elementName the name of the array element
     * @param aInner the type of the objects inside the array
     * @param innerElementName the element to extract from the objects of the array
     */
    def mapArrayValueExtractInt(
        p:                Project[?],
        a:                AnnotationLike,
        elementName:      String,
        aInner:           ClassType,
        innerElementName: String
    ): ArraySeq[Int] = {
        mapArrayValue(
            p,
            a,
            elementName,
            { annotation => getValue(p, aInner, annotation.elementValuePairs, innerElementName).asIntValue.value }
        )
    }

    /**
     * Extract a string and an int value from the objects of an array of an annotation.
     *
     * @param elementName the name of the array element
     * @param aInner the type of the objects inside the array
     * @param innerElementName1 the string element to extract from the objects of the array
     * @param innerElementName2 the int element to extract from the objects of the array
     */
    def mapArrayValueExtractStringAndInt(
        p:                 Project[?],
        a:                 AnnotationLike,
        elementName:       String,
        aInner:            ClassType,
        innerElementName1: String,
        innerElementName2: String
    ): ArraySeq[(String, Int)] = {
        mapArrayValue(
            p,
            a,
            elementName,
            { annotation =>
                (
                    getValue(p, aInner, annotation.elementValuePairs, innerElementName1).asStringValue.value,
                    getValue(p, aInner, annotation.elementValuePairs, innerElementName2).asIntValue.value
                )
            }
        )
    }

    /**
     * Extract two int values from the objects of an array of an annotation.
     *
     * @param elementName the name of the array element
     * @param aInner the type of the objects inside the array
     * @param innerElementName1 the first element to extract from the objects of the array
     * @param innerElementName2 the second element to extract from the objects of the array
     */

    def mapArrayValueExtractIntAndInt(
        p:                 Project[?],
        a:                 AnnotationLike,
        elementName:       String,
        aInner:            ClassType,
        innerElementName1: String,
        innerElementName2: String
    ): ArraySeq[(Int, Int)] = {
        mapArrayValue(
            p,
            a,
            elementName,
            { annotation =>
                (
                    getValue(p, aInner, annotation.elementValuePairs, innerElementName1).asIntValue.value,
                    getValue(p, aInner, annotation.elementValuePairs, innerElementName2).asIntValue.value
                )
            }
        )
    }

    /**
     * Check whether a collection of properties contains an [[BasicIDEProperty]] that satisfies a given predicate.
     */
    def existsBasicIDEProperty(properties: Iterable[Property], p: BasicIDEProperty[?, ?] => Boolean): Boolean = {
        properties.exists {
            case property: BasicIDEProperty[?, ?] => p(property)
            case _                                => false
        }
    }

    /**
     * Check whether a collection of properties contains an [[BasicIDEProperty]] that has a result entry that satisfies
     * a given predicate.
     */
    def existsBasicIDEPropertyResult(properties: Iterable[Property], p: ((?, ?)) => Boolean): Boolean = {
        existsBasicIDEProperty(properties, { property => property.results.exists(p(_)) })
    }
}
