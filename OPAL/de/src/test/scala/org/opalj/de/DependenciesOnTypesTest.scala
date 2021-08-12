/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package de

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br._
import org.opalj.br.reader.Java8Framework.ClassFile

/**
 * Tests that the dependency extractor extracts the types as desired.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DependenciesOnTypesTest extends AnyFlatSpec with Matchers {

    //
    //
    // Set up
    //
    //
    val extractedBaseTypes = scala.collection.mutable.Set.empty[BaseType]
    val extractedArrayTypes = scala.collection.mutable.Set.empty[ArrayType]
    val extractedObjectTypes = scala.collection.mutable.Set.empty[ObjectType]

    def dependencyExtractor: DependencyExtractor = {
        new DependencyExtractor(
            new DependencyProcessorAdapter {

                override def processDependency(
                    source: VirtualSourceElement,
                    target: VirtualSourceElement,
                    dType:  DependencyType
                ): Unit = {
                    if (target.isClass) {
                        val VirtualClass(targetType) = target
                        extractedObjectTypes += targetType
                    }
                }

                override def processDependency(
                    source:   VirtualSourceElement,
                    baseType: BaseType,
                    dType:    DependencyType
                ): Unit = {
                    extractedBaseTypes += baseType
                }

                override def processDependency(
                    source:    VirtualSourceElement,
                    arrayType: ArrayType,
                    dType:     DependencyType
                ): Unit = {
                    extractedArrayTypes += arrayType
                }
            }
        )
    }

    //
    //
    // EXERCISE
    //
    //
    val typesJAR = locateTestResources("types.jar", "bi")
    val classFiles = ClassFile(typesJAR, "types/TypeDeclarations.class")
    classFiles foreach (classFile => dependencyExtractor.process(classFile))

    //
    //
    // VERIFY
    //
    //

    behavior of "DependencyExtractor"

    it should "extract a dependency to java.lang.Object" in {
        assert(
            extractedObjectTypes contains ObjectType.Object,
            "the extractor did no report an existing dependency to java.lang.Object"
        )
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
