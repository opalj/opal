/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
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
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.resolved
package dependencies

import org.scalatest.FunSuite

/**
 * Tests that the association with IDs works as expected.
 *
 * @author Michael Eichberg
 */
//@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class SourceElementIDsTest extends FunSuite {

    import SourceElementIDs.{ sourceElementID ⇒ id }

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

    test("MethodDescriptor IDs") {
        val obj = id(ObjectType("java/lang/Object"))
        val int = id(ObjectType("java/lang/Integer"))
        val name = "foo"
        val md1 = MethodDescriptor("(III)V")
        val md2 = MethodDescriptor("(III)I")

        val obj_md1_id = id(obj, name, md1)
        assert(obj_md1_id == 1000000001)
        assert(obj_md1_id == id(obj, name, md1))

        val int_md1_id = id(int, name, md1)
        assert(int_md1_id > 1000000001)
        assert(int_md1_id == id(int, name, md1))
        assert(obj_md1_id != int_md1_id)

        val ind_md2_id = id(int, name, md2)
        assert(ind_md2_id > 1000000002)
        assert(ind_md2_id != int_md1_id)
        assert(ind_md2_id != id(int, "bar", md2))
    }
}
