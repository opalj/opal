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

import org.scalatest.FunSuite
import org.scalatest.ParallelTestExecution

/**
  * @author Michael Eichberg
  */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class MethodDescriptorTest extends FunSuite with ParallelTestExecution {

    test("Parsing: ()V") {
        val md = MethodDescriptor("()V")
        assert(md.parameterTypes.size == 0)
        assert(md.returnType.isVoidType)
    }

    test("Parsing: (III)I") {
        val md = MethodDescriptor("(III)I")
        assert(md.parameterTypes.size == 3)
        assert(md.parameterTypes(0).isIntegerType)
        assert(md.parameterTypes(1).isIntegerType)
        assert(md.parameterTypes(2).isIntegerType)
        assert(md.returnType.isIntegerType)
    }

    test("Parsing: ([I)[I") {
        val md = MethodDescriptor("([I)[I")
        assert(md.parameterTypes.size == 1)
        assert(md.parameterTypes(0).isArrayType)
        assert(md.returnType.isArrayType)
    }

    test("Parsing: ([[[III)[I") {
        val md = MethodDescriptor("([[[III)[I")
        assert(md.parameterTypes.size == 3)
        assert(md.parameterTypes(0).isArrayType)
        assert(md.parameterTypes(1).isIntegerType)
        assert(md.parameterTypes(2).isIntegerType)
        assert(md.returnType.isArrayType)
    }

    test("Parsing: (IDLjava/lang/Thread;)Ljava/lang/Object;") {
        val md = MethodDescriptor("(IDLjava/lang/Thread;)Ljava/lang/Object;")
        assert(md.parameterTypes.size == 3)
        assert(md.parameterTypes(0).isIntegerType)
        assert(md.parameterTypes(1).isDoubleType)
        assert(md.parameterTypes(2).isObjectType)
        assert(md.returnType.isObjectType)
    }

    test("Parsing: (IDLjava/lang/Thread;[J)[Ljava/lang/Object;") {
        val md = MethodDescriptor("(IDLjava/lang/Thread;[J)[Ljava/lang/Object;")
        assert(md.parameterTypes.size == 4)
        assert(md.parameterTypes(0).isIntegerType)
        assert(md.parameterTypes(1).isDoubleType)
        assert(md.parameterTypes(2).isObjectType)
        assert(md.parameterTypes(3).isArrayType)
        assert(md.returnType.isArrayType)
        assert(md match {
            case MethodDescriptor(Seq(_: BaseType, DoubleType, ObjectType(_), ArrayType(_)), ArrayType(_)) ⇒ true
            case _ ⇒ false
        })
    }

}
