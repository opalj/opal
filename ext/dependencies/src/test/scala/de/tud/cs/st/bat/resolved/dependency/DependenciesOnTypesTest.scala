/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package de.tud.cs.st
package bat
package resolved
package dependency

import reader.Java8Framework.ClassFile
import DependencyType._

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers

/**
 * Tests that the dependency extractor extracts the types as desired.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DependenciesOnTypesTest extends FlatSpec with Matchers {

    //
    //
    // Set up
    //
    //
    val extractedBaseTypes = scala.collection.mutable.Set.empty[BaseType]
    val extractedArrayTypes = scala.collection.mutable.Set.empty[ArrayType]
    val extractedObjectTypes = scala.collection.mutable.Set.empty[ObjectType]

    def dependencyExtractor =
        new DependencyExtractor(
            new DefaultDependencyProcessor {

                override def processDependency(
                    source: VirtualSourceElement,
                    target: VirtualSourceElement,
                    dType: DependencyType): Unit = {
                    if (target.isClass) {
                        val VirtualClass(targetType) = target
                        extractedObjectTypes += targetType
                    }
                }

                override def processDependency(
                    source: VirtualSourceElement,
                    baseType: BaseType,
                    dType: DependencyType): Unit =
                    extractedBaseTypes += baseType

                override def processDependency(
                    source: VirtualSourceElement,
                    arrayType: ArrayType,
                    dType: DependencyType): Unit =
                    extractedArrayTypes += arrayType
            }
        )

    //
    //
    // EXERCISE
    //
    //
    dependencyExtractor.process(
        ClassFile(
            TestSupport.locateTestResources("classfiles/Types.jar", "ext/dependencies"),
            "types/TypeDeclarations.class")
    )

    //
    //
    // VERIFY
    //
    //

    behavior of "DependencyExtractor"

    it should "extract a dependency to java.lang.Object" in {
        assert(
            extractedObjectTypes contains ObjectType.Object,
            "the extractor did no report an existing dependency to java.lang.Object")
    }

    it should "extract a dependency to the type java.lang.Object[]" in {
        assert(extractedArrayTypes contains ArrayType(ObjectType.Object))
    }

    it should "extract a dependency to the type java.lang.Object[][]" in {
        assert(extractedArrayTypes contains ArrayType(ArrayType(ObjectType.Object)))
    }

    it should "extract dependencies to byte" in {
        assert((extractedBaseTypes contains ByteType))
    }

    it should "extract dependencies to short" in {
        assert((extractedBaseTypes contains ShortType))
    }

    it should "extract dependencies to char" in {
        assert((extractedBaseTypes contains CharType))
    }

    it should "extract dependencies to int" in {
        assert((extractedBaseTypes contains IntegerType))
    }

    it should "extract dependencies to long" in {
        assert((extractedBaseTypes contains LongType))
    }

    it should "extract dependencies to boolean" in {
        assert((extractedBaseTypes contains BooleanType))
    }

    it should "extract dependencies to float" in {
        assert((extractedBaseTypes contains FloatType))
    }

    it should "extract dependencies to double" in {
        assert((extractedBaseTypes contains DoubleType))
    }

    it should "extract dependencies to byte arrays" in {
        assert((extractedArrayTypes contains ArrayType(ByteType)))
    }

    it should "extract dependencies to short arrays" in {
        assert((extractedArrayTypes contains ArrayType(ShortType)))
    }

    it should "extract dependencies to char arrays" in {
        assert((extractedArrayTypes contains ArrayType(CharType)))
    }

    it should "extract dependencies to int arrays" in {
        assert((extractedArrayTypes contains ArrayType(IntegerType)))
    }

    it should "extract dependencies to long arrays" in {
        assert((extractedArrayTypes contains ArrayType(LongType)))
    }

    it should "extract dependencies to boolean arrays" in {
        assert((extractedArrayTypes contains ArrayType(BooleanType)))
    }

    it should "extract dependencies to float arrays" in {
        assert((extractedArrayTypes contains ArrayType(FloatType)))
    }

    it should "extract dependencies to double arrays" in {
        assert((extractedArrayTypes contains ArrayType(DoubleType)))
    }

}