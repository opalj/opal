/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package org.opalj.fpcf
package analysis

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.opalj.br.Annotation
import org.opalj.br.analyses.Project
import java.net.URL
import org.opalj.br.EnumValue
import org.opalj.br.ElementValuePair
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.ObjectType
import org.opalj.br.Method
import org.opalj.br.ClassFile
import com.typesafe.config.ConfigFactory

/**
 *
 * Tests a fix-point analysis implementation using the classes in the configured
 * class file.
 *
 * @note This test supports only property tests where only one annotation field
 *       is used. It's not possible to check multiple values.
 *
 * @author Michael Reif
 */
@RunWith(classOf[JUnitRunner])
abstract class AbstractFixpointAnalysisTest extends FlatSpec with Matchers {

    /*
     * GENERIC TEST PARAMETERS - THESE HAVE TO BE OVERWRITTEN BY SUBCLASSES 
     */

    def analysisName: String

    def testFileName: String

    def testFilePath: String

    /**
     * This method has to be overridden in a subclass to define the analysis that
     * is going to be tested
     */
    def analysisRunner: FPCFAnalysisRunner[_]

    def runAnalysis(project: Project[URL]): Unit = {
        val executer = project.get(FPCFAnalysisManagerKey)
        executer.runAll(analysisRunner.recommendations)(false)
        executer.run(analysisRunner, true)
    }

    def propertyKey: PropertyKey

    def propertyAnnotation: ObjectType

    /**
     * Common default value of the annoation `propertyAnnotation` uses for the different
     * kinds of analysis assumptions. This value is used, if the annotated entity does not
     * provide an explicitly assigned value.
     */
    def defaultValue: String

    /*
     * PROJECT SETUP
     */

    def file = org.opalj.bi.TestSupport.locateTestResources(testFileName, testFilePath)

    def loadProject: Project[URL] = org.opalj.br.analyses.Project(file)

    val project = loadProject

    /*
     * RUN ANALYSIS AND OBTAIN PROPERTY STORE
     */

    runAnalysis(project)

    val propertyStore = project.get(SourceElementsPropertyStoreKey)
    propertyStore.waitOnPropertyComputationCompletion()

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
        annotation.elementValuePairs collectFirst (
            { case ElementValuePair("value", EnumValue(_, property)) ⇒ property }
        )
    }

    /**
     * This method belongs to the first for comprehension at the bottom of this test class.
     * It takes an annotated class and compares the annotated class property with the
     * computed property of the property store.
     */
    def validateProperty(classFile: ClassFile, annotation: Annotation): Unit = {

        val annotatedOProperty = propertyExtraction(annotation)

        val annotatedProperty = annotatedOProperty getOrElse (defaultValue)

        val computedOProperty = propertyStore(classFile, propertyKey)

        if (computedOProperty.isEmpty) {
            val className = classFile.fqn
            val message =
                "Class not found in PropertyStore:\n\t"+
                    s" { $className }\n\t\t has no property mapped to the respecting key: ${propertyKey};"+
                    s"\n\tclass name:      $className"+
                    s"\nexpected property: $annotatedProperty"
            fail(message)
        }

        val computedProperty = computedOProperty.get.toString

        if (computedProperty != annotatedProperty) {
            val className = classFile.fqn
            val message =
                "Wrong property computeted:\n\t"+
                    s" { $className } \n\t\thas the property $computedProperty mapped to the respecting key: $propertyKey;"+
                    s"\n\tclass name:        $className"+
                    s"\n\tactual property:   $computedProperty"+
                    s"\n\texpected property: $annotatedProperty"
            fail(message)
        }
    }

    /**
     * This method belongs to the second for comprehension at the bottom of this test class.
     * It takes an annotated method and compares the annotated method property with the
     * computed property of the property store.
     */
    def validateProperty(method: Method, annotation: Annotation): Unit = {

        val annotatedOProperty = propertyExtraction(annotation)

        val annotatedProperty = annotatedOProperty getOrElse (defaultValue)

        val computedOProperty = propertyStore(method, propertyKey)

        if (computedOProperty.isEmpty) {
            val className = project.classFile(method).fqn
            val message =
                "Method not found in PropertyStore:\n\t"+
                    className +
                    s" { ${method.toJava} }\n\t\t has no property mapped to the respecting key: ${propertyKey};"+
                    s"\n\tclass name:      $className"+
                    s"\n\tmethod:          ${method.toJava}"+
                    s"\nexpected property: $annotatedProperty"
            fail(message)
        }

        val computedProperty = computedOProperty.get.toString

        if (computedProperty != annotatedProperty) {
            val className = project.classFile(method).fqn
            val message =
                "Wrong property computeted:\n\t"+
                    className +
                    s" { $method } \n\t\thas the property $computedProperty mapped to the respecting key: $propertyKey;"+
                    s"\n\tclass name:        $className"+
                    s"\n\tmethod:            ${method.toJava}"+
                    s"\n\tactual property:   $computedProperty"+
                    s"\n\texpected property: $annotatedProperty"
            fail(message)
        }
    }

    /*
     * TESTS - test every method with the corresponding annotation
     */

    for {
        classFile ← project.allClassFiles
        annotation ← classFile.runtimeVisibleAnnotations
        if annotation.annotationType == propertyAnnotation
    } {
        analysisName should ("correctly calculate the property of the class "+classFile.fqn) in {

            validateProperty(classFile, annotation)
        }
    }

    for {
        classFile ← project.allClassFiles
        method ← classFile.methods
        annotation ← method.runtimeVisibleAnnotations
        if annotation.annotationType == propertyAnnotation
    } {
        analysisName should ("correctly calculate the property of the method "+
            method.toJava+" in class "+classFile.fqn) in {

                validateProperty(method, annotation)
            }
    }
}