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

import org.scalatest.FunSuite
import org.scalatest.ParallelTestExecution

/**
 * Tests that the association with IDs works as expected.
 *
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class SourceElementIDsTest extends FunSuite with ParallelTestExecution {

    val ids = new SourceElementIDsMap {}
    import ids.{ sourceElementID ⇒ id }

    test("BaseType IDs") {
        val it = id(IntegerType)
        val lt = id(LongType)
        assert(it != lt)
        assert(it == id(IntegerType))
    }

    test("ObjectType IDs") {
        val ot1 = id(ObjectType("java/lang/Object"))
        val ot2 = id(ObjectType("java/lang/Object"))
        assert(ot1 == ot2)

        val ot3 = id(ObjectType("java/lang/Integer"))
        assert(ot1 != ot3)
    }

    test("ArrayType IDs") {
        val ot1 = id(ObjectType("java/lang/Object"))
        val aot1 = id(FieldType("[Ljava/lang/Object;"))
        assert(ot1 != aot1)

        val aot2 = id(FieldType("[Ljava/lang/Object;"))
        assert(aot1 == aot2)

        val aot3 = id(FieldType("[Ljava/lang/Integer;"))
        assert(aot1 != aot3)
    }

    test("ArrayType IDs are replaced by ObjectType IDs") {
        val ids = new SourceElementIDsMap with UseIDOfBaseTypeForArrayTypes

        val ot1 = ids.sourceElementID(ObjectType("java/lang/Object"))
        val aot1 = ids.sourceElementID(FieldType("[Ljava/lang/Object;"))
        assert(ot1 == aot1)

        val aot2 = ids.sourceElementID(FieldType("[Ljava/lang/Object;"))
        assert(aot1 == aot2)

        val aot3 = ids.sourceElementID(FieldType("[Ljava/lang/Integer;"))
        assert(aot1 != aot3)

        assert(aot3 == ids.sourceElementID(ObjectType("java/lang/Integer")))
    }

    test("Method IDs") {
        val ids = new SourceElementIDsMap with UseIDOfBaseTypeForArrayTypes
        import ids.{ sourceElementID ⇒ id }

        val obj = ObjectType("java/lang/Object")
        val int = ObjectType("java/lang/Integer")
        val name = "foo"
        val md1 = MethodDescriptor("(III)V")
        val md2 = MethodDescriptor("(III)I")

        val obj_md1_id = id(obj, name, md1)
        assert(obj_md1_id == ids.LOWEST_METHOD_ID)
        assert(obj_md1_id == id(obj, name, md1))

        val int_md1_id = id(int, name, md1)
        assert(int_md1_id > ids.LOWEST_METHOD_ID)
        assert(int_md1_id == id(int, name, md1))
        assert(obj_md1_id != int_md1_id)

        val ind_md2_id = id(int, name, md2)
        assert(ind_md2_id > ids.LOWEST_METHOD_ID + 1)
        assert(ind_md2_id != int_md1_id)
        assert(ind_md2_id != id(int, "bar", md2))

        System.gc
        assert(obj_md1_id == id(obj, name, md1))
    }

    test("Field IDs") {
        val ids = new SourceElementIDsMap with UseIDOfBaseTypeForArrayTypes
        import ids.{ sourceElementID ⇒ id }

        val obj = ObjectType("java/lang/Object")
        val int = ObjectType("java/lang/Integer")
        val foo = "foo"
        val bar = "bar"

        val obj_foo_id = id(obj, foo)
        assert(obj_foo_id == ids.LOWEST_FIELD_ID)
        assert(obj_foo_id == id(obj, foo))

        val int_foo_id = id(int, foo)
        assert(int_foo_id > ids.LOWEST_FIELD_ID)
        assert(int_foo_id == id(int, foo))

        val ind_bar_id = id(int, bar)
        assert(ind_bar_id > ids.LOWEST_FIELD_ID + 1)
        assert(ind_bar_id != int_foo_id)
        assert(ind_bar_id == id(int, bar))
    }
}
