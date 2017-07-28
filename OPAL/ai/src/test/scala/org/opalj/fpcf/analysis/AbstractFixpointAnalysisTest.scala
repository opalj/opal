/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analysis

import org.junit.runner.RunWith
import org.opalj.br.Annotation
import org.opalj.br.AnnotationValue
import org.opalj.br.ArrayValue
import org.opalj.br.ClassFile
import org.opalj.br.ElementValuePair
import org.opalj.br.EnumValue
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.StringValue
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.PropertyStoreKey
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.scalatest.junit.JUnitRunner
import java.net.URL

/**
 * Tests a fix-point analysis implementation using the classes in the configured
 * class file.
 *
 * @note This test supports only property tests where only one annotation field
 *      is used. It's not possible to check multiple values.
 *
 * @author Michael Reif
 * @author Dominik Helm
 * @author Florian Kübler
 */
@RunWith(classOf[JUnitRunner])
abstract class AbstractFixpointAnalysisTest extends FlatSpec with Matchers {

    /**
     * Type of the EP annotation used for properties only valid
     * if certain requirements are fulfilled
     */
    val EPType = ObjectType("annotations/property/EP")

    /*
     * GENERIC TEST PARAMETERS - THESE HAVE TO BE OVERWRITTEN BY SUBCLASSES
     */

    def analysisName: String

    def testFileName: String

    def testFilePath: String

    /**
     * This method has to be overridden in a subclass to define the analysis that
     * is going to be tested.
     */
    def analysisRunner: FPCFAnalysisRunner

    def runAnalysis(project: Project[URL]): Unit = {
        val analysesManager = project.get(FPCFAnalysesManagerKey)
        analysesManager.runWithRecommended(analysisRunner)(waitOnCompletion = true)
    }

    def propertyKey: PropertyKey[Property]

    def propertyAnnotation: ObjectType

    /**
     * Common default value of the annoation `propertyAnnotation` uses for the different
     * kinds of analysis assumptions. This value is used, if the annotated entity does not
     * provide an explicitly assigned value.
     */
    def defaultValue: String

    /** Type of the container annotation: Override if propertyAnnotation is repeatable */
    def containerAnnotation: ObjectType = null

    /**
     * Provides a mapping from property name to the corresponding property key
     * Has to be overriden by tests to contain the properties the test depends on
     */
    def propertyKeyMap: Map[String, PropertyKey[Property]] = Map()

    /*
     * PROJECT SETUP
     */

    def file = org.opalj.bi.TestResources.locateTestResources(testFileName, testFilePath)

    def loadProject: Project[URL] = org.opalj.br.analyses.Project(file)

    val project = loadProject
    // The property stored initialization has to be lazy to ensure that subclasses have a
    // chance to configure the project before project information is actually derived.
    lazy val propertyStore = project.get(PropertyStoreKey)

    /**
     * This method is intended to be overridden by subclass to execute code before
     * {@link AbstractFixpointAnalysisTest#runAnalysis} called. The `project` is already
     * initialized and can be queried.
     */
    def init(): Unit = {}

    /*
     * RUN ANALYSIS AND OBTAIN PROPERTY STORE
     */

    init()
    runAnalysis(project)

    /*
     * PROPERTY VALIDATION
     */

    /**
     * This method extracts the default property, namely the ´value´ property
     * from the annotation and returns an option with a string. If the value
     * has no property or the default property has been set, the resulting
     * option will be empty.
     *
     * @note Subclasses should override this method when they use non-default
     * named values within their annotation. Please note that this extraction
     * mechanism can only be used if a single value has to be extracted.
     */
    def propertyExtraction(annotation: Annotation): Option[String] = {
        annotation.elementValuePairs collectFirst
            { case ElementValuePair("value", EnumValue(_, property)) ⇒ property }
    }

    /**
     * This method extracts the ep property if it is set and an array of Annotations
     * Otherwise the resulting option will be empty
     */
    def epExtraction(annotation: Annotation): Option[IndexedSeq[Annotation]] = {
        annotation.elementValuePairs collectFirst {
            case ElementValuePair("eps",
                ArrayValue(eps: IndexedSeq[_])) ⇒
                eps map { case AnnotationValue(annotation) ⇒ annotation }
        }
    }

    /**
     * This method extracts the algorithms property if it is set and an array of Strings
     * Otherwise the resulting option will be empty
     */
    def algorithmExtraction(annotation: Annotation): Option[IndexedSeq[String]] = {
        annotation.elementValuePairs collectFirst {
            case ElementValuePair("algorithms",
                ArrayValue(eps: IndexedSeq[_])) ⇒
                eps map { case StringValue(algorithm) ⇒ algorithm }
        }
    }

    /**
     * This method extracts a string value from an annotation that has a given name
     */
    def stringExtraction(annotation: Annotation, name: String): Option[String] = {
        annotation.elementValuePairs collectFirst {
            case ElementValuePair(`name`, StringValue(result)) ⇒ result
        }
    }

    /**
     * Extracts all annotations with an algorithm attribute that fulfills the predicate given
     * The input annotation is the test annotation that may be the container annotation
     * for reapeatable annotations
     */
    def extractAnnotations(
        annotation: Annotation,
        predicate:  Option[IndexedSeq[String]] ⇒ Boolean
    ): Seq[Annotation] = {
        annotation.elementValuePairs collectFirst {
            case ElementValuePair("value", EnumValue(_, _)) // Single flat annotation
            if predicate(algorithmExtraction(annotation)) ⇒ List(annotation)
            case ElementValuePair("value", ArrayValue(properties: IndexedSeq[_])) ⇒ // Container
                properties collect {
                    case AnnotationValue(annotation) if predicate(algorithmExtraction(annotation)) ⇒ annotation
                }
        } getOrElse List.empty[Annotation]
    }

    /**
     * Checks, whether all entities have the properties required for the annotation to be valid
     */
    def fulfillsEPS(annotation: Annotation): Boolean = {
        val eps = epExtraction(annotation)
        eps.isEmpty || eps.get.forall { ep ⇒
            if (ep.annotationType == EPType) {
                val epEntity = stringExtraction(ep, "e").get
                val epProperty = {
                    val key = stringExtraction(ep, "pk")
                    // If no property attribute is given, the property key tested is used
                    if (key.isDefined) propertyKeyMap(key.get) else propertyKey
                }
                val epValue = stringExtraction(ep, "p").get
                val classIndex = epEntity.indexOf('.')
                val descriptorIndex = epEntity.indexOf('(')
                if (classIndex < 0) { // Entity is a class
                    val classFileO = project.classFile(ObjectType(epEntity))
                    classFileO exists { classFile ⇒
                        val eop = propertyStore(classFile, epProperty)
                        eop.hasProperty && eop.p.toString == epValue
                    }
                } else if (descriptorIndex > classIndex) { // Entity is a method
                    val className = epEntity.substring(0, classIndex)
                    val name = epEntity.substring(classIndex + 1, descriptorIndex)
                    val descriptor = MethodDescriptor(epEntity.substring(descriptorIndex))
                    val classFileO = project.classFile(ObjectType(className))
                    classFileO exists {
                        _ findMethod (name, descriptor) exists { entity ⇒
                            val eop = propertyStore(entity, epProperty)
                            eop.hasProperty && eop.p.toString == epValue
                        }
                    }
                } else throw new RuntimeException("Invalid eps annotation")
            } else false
        }
    }

    /**
     * Returns the property expected by the annotation given
     * or None, if multiple annotations apply because of wrong annotation
     */
    def getExpectedProperty(annotation: Annotation): Option[String] = {
        val specific = extractAnnotations(annotation, { _.exists { _.exists { _ == analysisName } } })
        val specificFulfilled = specific filter fulfillsEPS
        val annotatedProperties =
            if (specificFulfilled.isEmpty)
                extractAnnotations(annotation, { _.isEmpty }) filter fulfillsEPS
            else
                specificFulfilled

        if (annotatedProperties.isEmpty)
            Some(defaultValue)
        else if (annotatedProperties.size == 1)
            Some(propertyExtraction(annotatedProperties(0)) getOrElse defaultValue)
        else
            None
    }

    /**
     * This method belongs to the first for comprehension at the bottom of this test class.
     * It takes an annotated class and compares the annotated class property with the
     * computed property of the property store.
     */
    def validatePropertyByClassFile(classFile: ClassFile, expected: String): Unit = {

        val computedOProperty = propertyStore(classFile, propertyKey)

        if (computedOProperty.hasNoProperty) {
            val className = classFile.fqn
            val message =
                "Class not found in PropertyStore:\n\t"+
                    s" { $className }\n\t\t has no property for $propertyKey;"+
                    s"\n\tclass name:      $className"+
                    s"\nexpected property: $expected"
            fail(message)
        }

        val computedProperty = computedOProperty.p.toString

        if (computedProperty != expected) {
            val className = classFile.fqn
            val message =
                "Wrong property computed:\n\t"+
                    s" { $className } \n\t\thas the property $computedProperty for $propertyKey;"+
                    s"\n\tclass name:        $className"+
                    s"\n\tactual property:   $computedProperty"+
                    s"\n\texpected property: $expected"
            fail(message)
        }
    }

    /**
     * This method belongs to the second for comprehension at the bottom of this test class.
     * It takes an annotated method and compares the annotated method property with the
     * computed property of the property store.
     */
    def validatePropertyByMethod(method: Method, expected: String): Unit = {

        assert(method ne null, "method is empty")

        val computedOProperty = propertyStore(method, propertyKey)

        if (computedOProperty.hasNoProperty) {
            val classFile = project.classFile(method)
            val message =
                s"Method has no property: ${classFile.fqn} for: $propertyKey;"+
                    s"\n\tmethod name:     ${method.name}"+
                    s"\nexpected property: $expected"
            fail(message)
        }

        val computedProperty = computedOProperty.p.toString

        if (computedProperty != expected) {
            val classFile = project.classFile(method)
            val message =
                "Wrong property computed: "+
                    s"${classFile.fqn} has the property $computedProperty for $propertyKey;"+
                    s"\n\tmethod name:       ${method.toJava(classFile)}"+
                    s"\n\tactual property:   $computedProperty"+
                    s"\n\texpected property: $expected"
            fail(message)
        }
    }

    /*
     * TESTS - test every class and method with the corresponding annotation
     */
    for {
        classFile ← project.allClassFiles
        annotation ← classFile.runtimeVisibleAnnotations
        if annotation.annotationType == propertyAnnotation
    } {
        val expectedPropertyO = getExpectedProperty(annotation)
        val expectedProperty = expectedPropertyO getOrElse {
            throw new RuntimeException("Test annotated incorrectly:"+
                s"Multiple annotations applicable for class ${classFile.fqn}")
        }

        val doWhat = s"correctly calculate the property of  ${classFile.fqn}: "+
            s"expected property ${expectedProperty}"
        analysisName should (doWhat) in { validatePropertyByClassFile(classFile, expectedProperty) }
    }

    for {
        classFile ← project.allClassFiles
        method ← classFile.methods
        annotation ← method.runtimeVisibleAnnotations
        if annotation.annotationType == propertyAnnotation ||
            annotation.annotationType == containerAnnotation
    } {
        val expectedPropertyO = getExpectedProperty(annotation)
        val expectedProperty = expectedPropertyO getOrElse {
            throw new RuntimeException("Test annotated incorrectly: "+
                "Multiple annotations applicable "+
                s"for method ${method.toJava(classFile)} in class ${classFile.fqn}")
        }

        val doWhat = s"correctly calculate the property of  ${method.toJava(classFile)}: "+
            s"expected property $expectedProperty"
        analysisName should (doWhat) in { validatePropertyByMethod(method, expectedProperty) }
    }
}
