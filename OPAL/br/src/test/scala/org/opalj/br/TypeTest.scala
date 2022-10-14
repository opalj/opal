/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.scalatest.funspec.AnyFunSpec
import org.scalatestplus.junit.JUnitRunner
import org.junit.runner.RunWith

@RunWith(classOf[JUnitRunner])
class TypeTest extends AnyFunSpec {

    describe("types") {
        it("should be properly initialized on first usage") {
            Seq(ByteType, CharType, ShortType, IntegerType, LongType, FloatType, DoubleType, BooleanType) foreach { t =>
                val wt = t.WrapperType
                val pt = ObjectType.primitiveType(wt)
                assert(pt.isDefined, s"primitive type lookup failed (${t.WrapperType.toJava})")
                assert(pt.get != null, s"primitive type for ${t.WrapperType.toJava} was null")
            }
        }
    }

    describe("ObjectType") {
        it("should remove all non-predefined types on flush") {

            ObjectType.flushTypeCache()
            assert(ObjectType.objectTypesCount == ObjectType.highestPredefinedTypeId + 1)

            // Create 100 dummy OTs and assert their ID assignment is correct
            for (i <- 1 to 100) {
                val t = ObjectType(s"some/type/name$i")
                assert(t.id == ObjectType.highestPredefinedTypeId + i, "invalid id for newly constructed type")
            }

            // Check that highest OT ID is in fact present and can be looked up
            try {
                ObjectType.lookup(ObjectType.highestPredefinedTypeId + 99)
            } catch {
                case _: Exception =>
                    fail(s"defined type with id ${ObjectType.highestPredefinedTypeId + 99} was not found in cache")
            }

            // Flush OT cache and assert ID counter is reset
            ObjectType.flushTypeCache()
            assert(ObjectType.objectTypesCount == ObjectType.highestPredefinedTypeId + 1)

            // Assert ID was properly reset and new OTs are assigned an id starting from the predefined types
            val newType = ObjectType("some/other/type")
            assert(
                newType.id == ObjectType.highestPredefinedTypeId + 1,
                "invalid id for newly constructed type after flush"
            )

            // Assert pre-flush IDs are no longer valid
            assertThrows[IllegalArgumentException](ObjectType.lookup(ObjectType.highestPredefinedTypeId + 99))
        }

        it("should not remove predefined types on flush") {
            ObjectType.flushTypeCache()

            // Check that first and last predefined OT are still present after flush
            val obj = ObjectType.lookup(ObjectType.ObjectId)
            assert(obj.fqn.equals("java/lang/Object"))

            val oos = ObjectType.lookup(ObjectType.highestPredefinedTypeId)
            assert(oos.fqn.equals("java/io/ObjectOutputStream"))
        }
    }

    describe("ArrayType") {
        it("should remove all non-predefined types on flush") {
            ArrayType.flushTypeCache()

            // Create dummy ATs and assert their IDs are correct
            // Note: Use dimensions = 1 here, otherwise the apply function will internally create more ATs
            val objAT = ArrayType(1, ObjectType("some/type/name"))
            assert(objAT.id == ArrayType.lowestPredefinedTypeId - 1)

            val intAT = ArrayType(1, ObjectType.Integer)
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
            val newAT = ArrayType(1, ObjectType("some/other/name"))
            assert(newAT.id == ArrayType.lowestPredefinedTypeId - 1)

            // Assert pre-flush IDs are no longer valid
            assertThrows[IllegalArgumentException](ArrayType.lookup(ArrayType.lowestPredefinedTypeId - 2))
        }
        it("should not remove predefined types on flush") {
            ArrayType.flushTypeCache()

            // Check that both predefined ATs are still present after flush
            val objAT = ArrayType.lookup(-1)
            assert(objAT.dimensions == 1 && objAT.componentType.equals(ObjectType.Object))
            val mhAT = ArrayType.lookup(-2)
            assert(mhAT.dimensions == 1 && mhAT.componentType.equals(ObjectType.MethodHandle))
        }
    }

}
