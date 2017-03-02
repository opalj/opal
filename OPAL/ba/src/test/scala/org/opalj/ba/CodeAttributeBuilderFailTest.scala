/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package org.opalj
package ba

import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import org.opalj.br.instructions.IFGE
import org.opalj.br.instructions.ILOAD_2
import org.opalj.br.instructions.IRETURN
import org.opalj.br.instructions.LOOKUPSWITCH
import org.opalj.br.instructions.RETURN

/**
 * Tests the require statements and warnings of a CodeAttributeBuilder.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class CodeAttributeBuilderFailTest extends FlatSpec {
    behavior of "CodeAttributeBuilder requires"

    val stream = new java.io.ByteArrayOutputStream()
    Console.withOut(stream) {
        val failMethod = PUBLIC("test", "(II)", "I")(
            CODE(
                ILOAD_2,
                IRETURN
            ) MAXSTACK 0 MAXLOCALS 0
        ).buildMethod._1

        val warnMessage = s"you defined %s of method "+
            s"'${failMethod.descriptor.toJava(failMethod.name)}' too small;"+
            "explicitly configured value is nevertheless kept"

        "the CodeAttributeBuilder" should "warn about a too small defined max_locals value" in {
            assert(stream.toString.contains(warnMessage.format("max_locals")))
        }

        it should "warn about a too small defined max_stack value" in {
            assert(stream.toString.contains(warnMessage.format("max_stack")))
        }
    }

    it should "fail with no instructions added" in {
        assertThrows[IllegalArgumentException](CODE())
        assertThrows[IllegalArgumentException](CODE('notAnInstruction))
    }

    it should "fail with duplicated labels" in {
        assertThrows[IllegalArgumentException](
            CODE(
                'label,
                'label,
                RETURN
            )
        )
    }

    it should "fail with unresolvable labels in branch instructions" in {
        assertThrows[IllegalArgumentException](CODE(IFGE('label)))
        assertThrows[IllegalArgumentException](
            CODE(
                'default,
                LOOKUPSWITCH('default, IndexedSeq((0, 'label)))
            )
        )
        assertThrows[IllegalArgumentException](
            CODE(
                'default,
                'label1,
                LOOKUPSWITCH('default, IndexedSeq((0, 'label1), (0, 'label2)))
            )
        )
    }
}