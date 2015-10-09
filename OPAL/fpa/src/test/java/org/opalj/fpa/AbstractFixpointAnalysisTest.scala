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
package org.opalj.fpa

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.opalj.fp.PropertyKey
import org.opalj.br.ObjectType
import org.opalj.br.Annotation
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import java.net.URL
import org.opalj.br.EnumValue
import org.opalj.br.ElementValuePair
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import com.typesafe.config.ConfigFactory
import org.opalj.AnalysisModes
import com.typesafe.config.Config

/**
 * Simple factory that can create a new Config by a given analysis mode. This is necessary
 * for test purposes because the analysis mode, which is configured in the configuration file,
 * has to be ignored to implement config file independet tests.
 */
object TestConfigFactory {

    private[this] final val cpaConfig =
        "org.opalj { analysisMode = \"Library with closed packages assumption\"}"

    private[this] final val opaConfig =
        "org.opalj { analysisMode = \"Library with open packages assumption\"}"

    private[this] final val appConfig =
        "org.opalj { analysisMode = \"application\"}"

    def createConfig(value: AnalysisModes.Value): Config = {
        value match {
            case AnalysisModes.LibraryWithOpenPackagesAssumption   ⇒ ConfigFactory.parseString(opaConfig)
            case AnalysisModes.LibraryWithClosedPackagesAssumption ⇒ ConfigFactory.parseString(cpaConfig)
            case AnalysisModes.Application                         ⇒ ConfigFactory.parseString(appConfig)
        }
    }
}

/**
 *
 * Tests a fix-point analysis implementation using the classes in the configured
 * class file.
 *
 * @author Michael Reif
 */
@RunWith(classOf[JUnitRunner])
abstract class AbstractFixpointAnalysisTest extends FlatSpec with Matchers {

    /*
     * GENERIC TEST PARAMETERS - THESE HAVE TO BE OVERWRITTEN BY SUBCLASSES 
     */

    val analysisName: String

    def testFileName: String

    def testFilePath: String

    /**
     * This method has to be implement in the subclasses. This is in particular import for
     * analyses that hardly depend on the output of other analyses.
     */
    def runAnalysis(project: Project[URL]): Unit

    def propertyKey: PropertyKey

    def propertyAnnotation: ObjectType

    /**
     * Common default value of the annoation `propertyAnnotation` uses for the different
     * kinds of analysis assumptions. This value is used, if the annotated entity does not
     * provide an explicitly assigned value.
     */
    val defaultValue: String

    def analysisMode: AnalysisModes.Value

    /*
     * PROJECT SETUP
     */

    def file = org.opalj.bi.TestSupport.locateTestResources(testFileName, testFilePath)
    val project = org.opalj.br.analyses.Project(file)

    project.config.resolveWith(TestConfigFactory.createConfig(analysisMode))

    /*
     * RUN ANALYSIS AND OBTAIN PROPERTY STORE
     */

    runAnalysis(project)

    val propertyStore = project.get(SourceElementsPropertyStoreKey)
    propertyStore.waitOnPropertyComputationCompletion()

    /*
     * PROPERTY VALIDATION
     */

    def validateProperty(method: Method, annotation: Annotation): Unit = {

        val annotatedOProperty = analysisMode match {
            case AnalysisModes.LibraryWithOpenPackagesAssumption ⇒
                annotation.elementValuePairs collectFirst (
                    { case ElementValuePair("opa", EnumValue(_, property)) ⇒ property })
            case AnalysisModes.LibraryWithClosedPackagesAssumption ⇒
                annotation.elementValuePairs collectFirst (
                    { case ElementValuePair("cpa", EnumValue(_, property)) ⇒ property })
            case AnalysisModes.Application ⇒
                annotation.elementValuePairs collectFirst (
                    { case ElementValuePair("application", EnumValue(_, property)) ⇒ property })

        }

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