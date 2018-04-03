/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
import org.opalj.ai.domain.l0.TypeCheckingDomain
import org.opalj.ai.util.XHTML
import org.opalj.bc.Assembler
import org.opalj.bi.ACC_PUBLIC
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.instructions._
import org.opalj.util.InMemoryClassLoader

/**
 * Tests the require statements and warnings of a CodeAttributeBuilder.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class CodeAttributeBuilderTest extends FlatSpec {

    final val FakeObjectType = ObjectType("<FAKE_TYPE>") // this type name is NOT valid

    behavior of "CodeAttributeBuilder when the code is invalid"

    "the CodeAttributeBuilder" should "warn about a too small max_locals/max_stack values" in {
        implicit val ch = br.Code.BasicClassHierarchy
        val md = MethodDescriptor("(II)I")
        val code = (CODE(ILOAD_2, IRETURN) MAXSTACK 0 MAXLOCALS 0)
        val (_, (_, warnings)) = code(bi.Java5Version, FakeObjectType, ACC_PUBLIC.mask, "test", md)

        assert(warnings.size == 2)
    }

    it should "fail when there are no instructions" in {
        assertThrows[IllegalArgumentException](CODE())
        assertThrows[IllegalArgumentException](CODE('notAnInstruction))
    }

    it should "fail with duplicated labels" in {
        assertThrows[IllegalArgumentException](CODE('label, 'label, RETURN))
    }

    it should "fail with unresolvable labels in branch instructions" in {
        assertThrows[java.util.NoSuchElementException](CODE(IFGE('label)))
        assertThrows[java.util.NoSuchElementException](
            CODE('default, LOOKUPSWITCH('default, IndexedSeq((0, 'label))))
        )
        assertThrows[java.util.NoSuchElementException](
            CODE('default, 'label1, LOOKUPSWITCH('default, IndexedSeq((0, 'label1), (0, 'label2))))
        )
    }

    it should "be able to compute the correct stack map table" in {

        val StringType = ObjectType.String
        val codeElements = Array[CodeElement[AnyRef]](
            LabelElement(PCLabel(0)),
            ALOAD_0,
            LabelElement(PCLabel(1)),
            NOP, // INVOKESTATIC(effekt.Effekt{ void beforeEffect() }),
            POP, ICONST_1, // INVOKEINTERFACE(run.amb.Amb{ boolean flip() }),
            ICONST_1, // INVOKESTATIC(effekt.Effekt{ boolean isEffectful() }),
            LabeledIFEQ('EPResume1),
            POP,
            ALOAD_0,
            POP, ACONST_NULL, // INVOKEDYNAMIC(enter(run.amb.Amb)),
            POP, // INVOKESTATIC(effekt.Effekt{ void push(effekt.Frame) }),
            ACONST_NULL,
            ARETURN,
            LabelElement('EP1),
            ALOAD_0,
            ASTORE_0,
            ICONST_1, // INVOKESTATIC(effekt.Effekt{ int resultI() }),
            LabelElement('EPResume1),
            LabelElement(PCLabel(6)),
            LabeledIFEQ(PCLabel(26)),
            LabelElement(PCLabel(9)),
            ALOAD_0,
            LabelElement(PCLabel(10)),
            NOP, // INVOKESTATIC(effekt.Effekt{ void beforeEffect() }),
            POP, ICONST_1, // INVOKEINTERFACE(run.amb.Amb{ boolean flip() }),
            ICONST_1, // INVOKESTATIC(effekt.Effekt{ boolean isEffectful() }),
            LabeledIFEQ('EPResume2),
            POP,
            ACONST_NULL, // INVOKEDYNAMIC(enter()),
            POP, // INVOKESTATIC(effekt.Effekt{ void push(effekt.Frame) }),
            ACONST_NULL,
            ARETURN,
            LabelElement('EP2),
            ICONST_1, // INVOKESTATIC(effekt.Effekt{ int resultI() }),
            LabelElement('EPResume2),
            LabelElement(PCLabel(15)),
            LabeledIFEQ(PCLabel(23)),
            LabelElement(PCLabel(18)),
            LoadString("Heads"),
            LabelElement(PCLabel(20)),
            LabeledGOTO(PCLabel(25)),
            LabelElement(PCLabel(23)),
            LoadString("Tails"),
            LabelElement(PCLabel(25)),
            DUP,
            POP, // INVOKESTATIC(effekt.Effekt{ void returnWith(java.lang.Object) }),
            CHECKCAST(StringType),
            ARETURN,
            LabelElement(PCLabel(26)),
            LoadString("Dropped"),
            LabelElement(PCLabel(28)),
            DUP,
            POP, // INVOKESTATIC(effekt.Effekt{ void returnWith(java.lang.Object) }),
            CHECKCAST(StringType),
            ARETURN,
            LabelElement(PCLabel(29))
        )
        val c = CODE[AnyRef](codeElements)

        val classBuilder = CLASS(
            version = org.opalj.bi.Java8Version,
            accessModifiers = PUBLIC,
            thisType = "TheClass",
            methods = METHODS(
                METHOD(
                    PUBLIC, "<init>", "()V",
                    CODE[AnyRef](
                        LINENUMBER(0),
                        ALOAD_0,
                        LINENUMBER(1),
                        INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                        'return,
                        LINENUMBER(2),
                        RETURN
                    ) MAXSTACK 2 MAXLOCALS 3
                ),
                METHOD(PUBLIC, "tryCatchFinallyTest", "(Ljava/lang/String;)Ljava/lang/String;", c)
            )
        )

        val (brClassFile, _) = classBuilder.toBR()
        val daClassFile = ba.toDA(brClassFile)
        val rawClassFile = Assembler(daClassFile)

        val loader = new InMemoryClassLoader(
            Map("TheClass" → rawClassFile), this.getClass.getClassLoader
        )

        val clazz = loader.loadClass("TheClass")
        try {
            val clazzInstance = clazz.newInstance()
            val clazzMethod = clazz.getMethod("tryCatchFinallyTest", classOf[String])
            clazzMethod.invoke(clazzInstance, "test")
        } catch {
            case e: VerifyError ⇒
                val theMethod = brClassFile.findMethod("tryCatchFinallyTest").head
                val theCode = theMethod.body.get
                val theSMT = theCode.stackMapTable.get

                info(e.toString)
                info(codeElements.mkString("Code Elements:\n\t\t", "\n\t\t", "\n\t"))
                info(
                    theCode.
                        instructions.zipWithIndex.filter(_._1 != null).
                        map(_.swap).mkString("Instructions:\n\t\t", "\n\t\t", "\n")
                )
                info(theSMT.pcs.mkString("Stack map table pcs: ", ", ", ""))
                info(theSMT.stackMapFrames.mkString("Stack map table entries:\n\t\t", "\n\t\t", "\n"))

                val theDomain = new TypeCheckingDomain(br.Code.BasicClassHierarchy, theMethod)
                val ils = CodeAttributeBuilder.ai.initialLocals(theMethod, theDomain)(None)
                val ios = CodeAttributeBuilder.ai.initialOperands(theMethod, theDomain)
                val r = CodeAttributeBuilder.ai.performInterpretation(theCode, theDomain)(ios, ils)
                org.opalj.io.writeAndOpen(
                    org.opalj.ai.common.XHTML.dump(
                        Some(brClassFile),
                        Some(theMethod),
                        theCode,
                        Some(
                            XHTML.instructionsToXHTML("PCs where paths join", r.cfJoins) +
                                XHTML.evaluatedInstructionsToXHTML(r.evaluated)
                        ),
                        r.domain
                    )(r.cfJoins, r.operandsArray, r.localsArray),
                    "AIResult",
                    ".html"
                )
                fail(e)
        }
    }
}
