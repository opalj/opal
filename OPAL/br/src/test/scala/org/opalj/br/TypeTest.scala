/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.junit.runner.RunWith
import org.scalatest.funspec.AnyFunSpec
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TypeTest extends AnyFunSpec {

    describe("types") {
        it("should be properly initialized on first usage") {
            Seq(ByteType, CharType, ShortType, IntegerType, LongType, FloatType, DoubleType, BooleanType) foreach { t =>
                val wt = t.WrapperType
                val pt = ClassType.primitiveType(wt)
                assert(pt.isDefined, s"primitive type lookup failed (${t.WrapperType.toJava})")
                assert(pt.get != null, s"primitive type for ${t.WrapperType.toJava} was null")
            }
        }
    }

    describe("ClassType") {
        it("should remove all non-predefined types on flush") {

            ClassType.flushTypeCache()
            assert(ClassType.classTypesCount == ClassType.highestPredefinedTypeId + 1)

            // Create 100 dummy CTs and assert their ID assignment is correct
            for (i <- 1 to 100) {
                val t = ClassType(s"some/type/name$i")
                assert(t.id == ClassType.highestPredefinedTypeId + i, "invalid id for newly constructed type")
            }

            // Check that highest CT ID is in fact present and can be looked up
            try {
                ClassType.lookup(ClassType.highestPredefinedTypeId + 99)
            } catch {
                case _: Exception =>
                    fail(s"defined type with id ${ClassType.highestPredefinedTypeId + 99} was not found in cache")
            }

            // Flush CT cache and assert ID counter is reset
            ClassType.flushTypeCache()
            assert(ClassType.classTypesCount == ClassType.highestPredefinedTypeId + 1)

            // Assert ID was properly reset and new CTs are assigned an id starting from the predefined types
            val newType = ClassType("some/other/type")
            assert(
                newType.id == ClassType.highestPredefinedTypeId + 1,
                "invalid id for newly constructed type after flush"
            )

            // Assert pre-flush IDs are no longer valid
            assertThrows[IllegalArgumentException](ClassType.lookup(ClassType.highestPredefinedTypeId + 99))
        }

        it("should not remove predefined types on flush") {
            ClassType.flushTypeCache()

            // Check that first and last predefined CT are still present after flush
            val obj = ClassType.lookup(ClassType.ObjectId)
            assert(obj.fqn.equals("java/lang/Object"))

            val oos = ClassType.lookup(ClassType.highestPredefinedTypeId)
            assert(oos.fqn.equals("java/io/ObjectOutputStream"))
        }
    }

    describe("ArrayType") {
        it("should remove all non-predefined types on flush") {
            ArrayType.flushTypeCache()

            // Create dummy ATs and assert their IDs are correct
            // Note: Use dimensions = 1 here, otherwise the apply function will internally create more ATs
            val objAT = ArrayType(1, ClassType("some/type/name"))
            assert(objAT.id == ArrayType.lowestPredefinedTypeId - 1)

            val intAT = ArrayType(1, ClassType.Integer)
            assert(intAT.id == ArrayType.lowestPredefinedTypeId - 2)

            // Check that highest AT ID is in fact present and can be looked up
            try { ArrayType.lookup(ArrayType.lowestPredefinedTypeId - 2) }
            catch {
                case _: Exception =>
                    fail(s"defined AT with id ${ArrayType.lowestPredefinedTypeId - 2} was not found in cache")
            }

            // Flush AT Cache
            ArrayType.flushTypeCache()

            // Assert ID was properly reset and new ATs are assigned an id starting from the predefined types
            val newAT = ArrayType(1, ClassType("some/other/name"))
            assert(newAT.id == ArrayType.lowestPredefinedTypeId - 1)

            // Assert pre-flush IDs are no longer valid
            assertThrows[IllegalArgumentException](ArrayType.lookup(ArrayType.lowestPredefinedTypeId - 2))
        }
        it("should not remove predefined types on flush") {
            ArrayType.flushTypeCache()

            // Check that both predefined ATs are still present after flush
            val objAT = ArrayType.lookup(-1)
            assert(objAT.dimensions == 1 && objAT.componentType.equals(ClassType.Object))
            val mhAT = ArrayType.lookup(-2)
            assert(mhAT.dimensions == 1 && mhAT.componentType.equals(ClassType.MethodHandle))
        }
    }

}
