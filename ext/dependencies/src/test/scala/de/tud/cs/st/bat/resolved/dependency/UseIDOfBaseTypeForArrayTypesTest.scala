/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package dependency

import reader.Java7Framework
import DependencyType._

import org.junit.runner.RunWith

import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers

/**
 * Tests that the dependency extractor does not report dependencies to array types but
 * to their base types instead.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class UseIDOfBaseTypeForArrayTypesTest
        extends FlatSpec
        with ShouldMatchers
        with BeforeAndAfterAll {

    //
    //
    // Set up
    //
    //

    val extractedTypes = scala.collection.mutable.Set[Type]()

    class SourceElementIDsProvider extends SourceElementIDs {

        def sourceElementID(t: Type): Int = {
            extractedTypes += t
            -1
        }

        def sourceElementID(definingObjectType: ObjectType, fieldName: String): Int = {
            extractedTypes += definingObjectType
            -1
        }

        def sourceElementID(definingReferenceType: ReferenceType,
                            methodName: String,
                            methodDescriptor: MethodDescriptor): Int = {
            extractedTypes += definingReferenceType
            -1
        }
    }

    object TypeCollector
            extends DependencyExtractor(new SourceElementIDsProvider with UseIDOfBaseTypeForArrayTypes)
            with NoSourceElementsVisitor {
        def processDependency(sourceID: Int, targetID: Int, dependencyType: DependencyType) {}
    }

    //
    //
    // EXERCISE
    //
    //

    TypeCollector.process(
        Java7Framework.ClassFile(
            TestSupport.locateTestResources("classfiles/Dependencies.jar", "ext/dependencies"),
            "dependencies/InstructionsTestClass.class")
    )

    //
    //
    // VERIFY
    //
    //

    behavior of "DependencyExtractor with UseIDOfBaseTypeForArrayTypes"

    it should "extract a dependency to java.lang.Object" in {
        assert(extractedTypes contains ObjectType("java/lang/Object"))
    }

    it should "extract a dependency to java.lang.Long" in {
        assert(extractedTypes contains ObjectType("java/lang/Long"))
    }

    it should "extract a dependency to java.lang.Integer" in {
        assert(extractedTypes contains ObjectType("java/lang/Integer"))
    }

    it should "not extract a dependency to any arraytype" in {
        assert(!(extractedTypes exists { case ArrayType(_) ⇒ true; case _ ⇒ false }))
    }

    //    it should "never extract dependencies to array types" in {
    //        var files = new java.io.File("../../../../../../../test/classfiles").listFiles()
    //        if (files == null) files = new java.io.File("test/classfiles").listFiles()
    //        for {
    //            file ← files
    //            if (file.isFile && file.canRead && file.getName.endsWith(".zip"))
    //        } {
    //            val zipfile = new java.util.zip.ZipFile(file)
    //            val zipentries = (zipfile).entries
    //            while (zipentries.hasMoreElements) {
    //                val zipentry = zipentries.nextElement
    //                if (!zipentry.isDirectory && zipentry.getName.endsWith(".class")) {
    //                    var classFile = Java6Framework.ClassFile(() ⇒ zipfile.getInputStream(zipentry))
    //                    TypeCollector.process(classFile)
    //                }
    //            }
    //        }
    //        assert(!(extractedTypes exists { case ArrayType(_) ⇒ true; case _ ⇒ false }))
    //    }

}