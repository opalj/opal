/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner
import org.opalj.util.InMemoryClassLoader
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bc.Assembler
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.instructions._
import org.opalj.br.ClassHierarchy
import org.opalj.ai.domain.l0.TypeCheckingDomain
import org.opalj.ai.util.XHTML
import org.opalj.br.BooleanType
import org.opalj.br.ExceptionHandler
import org.opalj.br.MethodDescriptor.JustTakes

import scala.collection.immutable.ArraySeq

/**
 * Tests the require statements and warnings of a CodeAttributeBuilder.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class CodeAttributeBuilderTest extends AnyFlatSpec {

    final val FakeObjectType = ObjectType("<FAKE_TYPE>") // this type name is NOT valid

    behavior of "CodeAttributeBuilder when the code is invalid"

    "the CodeAttributeBuilder" should "warn about a too small max_locals/max_stack values" in {
        implicit val ch = br.ClassHierarchy.PreInitializedClassHierarchy
        val md = MethodDescriptor("(II)I")
        val code = (CODE(ILOAD_2, IRETURN) MAXSTACK 0 MAXLOCALS 0)
        val (_, (_, warnings)) = code(bi.Java5Version, FakeObjectType, ACC_PUBLIC.mask, "test", md)

        assert(warnings.size == 2)
    }

    it should "fail when there are no instructions" in {
        assertThrows[IllegalArgumentException](CODE())
        assertThrows[IllegalArgumentException](CODE(Symbol("notAnInstruction")))
    }

    it should "fail with duplicated labels" in {
        assertThrows[IllegalArgumentException](CODE(Symbol("label"), Symbol("label"), RETURN))
    }

    it should "fail with unresolvable labels in branch instructions" in {
        assertThrows[java.util.NoSuchElementException](CODE(IFGE(Symbol("label"))))
        assertThrows[java.util.NoSuchElementException](
            CODE(Symbol("default"), LOOKUPSWITCH(Symbol("default"), ArraySeq((0, Symbol("label")))))
        )
        assertThrows[java.util.NoSuchElementException](
            CODE(Symbol("default"), Symbol("label1"), LOOKUPSWITCH(Symbol("default"), ArraySeq((0, Symbol("label1")), (0, Symbol("label2")))))
        )
    }

    def testEvaluation(
        codeElements:   Array[CodeElement[AnyRef]],
        theBRClassFile: br.ClassFile,
        theBRMethod:    br.Method
    )(f: => Unit): Unit = {
        try {
            f
        } catch {
            case e: VerifyError =>
                import ClassHierarchy.PreInitializedClassHierarchy
                val theCode = theBRMethod.body.get
                val theSMT = theCode.stackMapTable.get

                info(e.toString)
                info(
                    codeElements.
                        zipWithIndex.
                        map(_.swap).
                        mkString("Code Elements:\n\t\t", "\n\t\t", "\n\t")
                )
                info(
                    theCode.
                        instructions.zipWithIndex.filter(_._1 != null).
                        map(_.swap).
                        mkString("Instructions:\n\t\t", "\n\t\t", "\n")
                )
                info(
                    theCode.exceptionHandlers.mkString("Exception Handlers:\n\t\t", "\n\t\t", "\n")
                )
                info(
                    theCode.liveVariables(PreInitializedClassHierarchy).
                        zipWithIndex.filter(_._1 != null).map(_.swap).
                        mkString("Live variables:\n\t\t", "\n\t\t", "\n")
                )
                info(theSMT.pcs.mkString("Stack map table pcs: ", ", ", ""))
                info(theSMT.stackMapFrames.mkString("Stack map table entries:\n\t\t", "\n\t\t", "\n"))

                val theDomain = new TypeCheckingDomain(PreInitializedClassHierarchy, theBRMethod)
                val ils = CodeAttributeBuilder.ai.initialLocals(theBRMethod, theDomain)(None)
                val ios = CodeAttributeBuilder.ai.initialOperands(theBRMethod, theDomain)
                val r = CodeAttributeBuilder.ai.performInterpretation(theCode, theDomain)(ios, ils)
                org.opalj.io.writeAndOpen(
                    org.opalj.ai.common.XHTML.dump(
                        Some(theBRClassFile),
                        Some(theBRMethod),
                        theCode,
                        Some(
                            XHTML.instructionsToXHTML("PCs where paths join", r.cfJoins).toString() +
                                XHTML.evaluatedInstructionsToXHTML(r.evaluatedPCs)
                        ),
                        r.domain
                    )(r.cfJoins, r.operandsArray, r.localsArray),
                    "AIResult",
                    ".html"
                )
                fail(e)
        }
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
            LabeledIFEQ(Symbol("EPResume1")),
            POP,
            ALOAD_0,
            POP, ACONST_NULL, // INVOKEDYNAMIC(enter(run.amb.Amb)),
            POP, // INVOKESTATIC(effekt.Effekt{ void push(effekt.Frame) }),
            ACONST_NULL,
            ARETURN,
            LabelElement(Symbol("EP1")),
            ALOAD_0,
            ASTORE_0,
            ICONST_1, // INVOKESTATIC(effekt.Effekt{ int resultI() }),
            LabelElement(Symbol("EPResume1")),
            LabelElement(PCLabel(6)),
            LabeledIFEQ(PCLabel(26)),
            LabelElement(PCLabel(9)),
            ALOAD_0,
            LabelElement(PCLabel(10)),
            NOP, // INVOKESTATIC(effekt.Effekt{ void beforeEffect() }),
            POP, ICONST_1, // INVOKEINTERFACE(run.amb.Amb{ boolean flip() }),
            ICONST_1, // INVOKESTATIC(effekt.Effekt{ boolean isEffectful() }),
            LabeledIFEQ(Symbol("EPResume2")),
            POP,
            ACONST_NULL, // INVOKEDYNAMIC(enter()),
            POP, // INVOKESTATIC(effekt.Effekt{ void push(effekt.Frame) }),
            ACONST_NULL,
            ARETURN,
            LabelElement(Symbol("EP2")),
            ICONST_1, // INVOKESTATIC(effekt.Effekt{ int resultI() }),
            LabelElement(Symbol("EPResume2")),
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
                        Symbol("return"),
                        LINENUMBER(2),
                        RETURN
                    ) MAXSTACK 2 MAXLOCALS 3
                ),
                METHOD(PUBLIC, "tryCatchFinallyTest", "(Ljava/lang/String;)Ljava/lang/String;", c)
            )
        )
        val (brClassFile, _) = classBuilder.toBR()
        val brMethod = brClassFile.findMethod("tryCatchFinallyTest").head
        val daClassFile = ba.toDA(brClassFile)
        val rawClassFile = Assembler(daClassFile)

        val loader = new InMemoryClassLoader(
            Map("TheClass" -> rawClassFile), this.getClass.getClassLoader
        )
        val clazz = loader.loadClass("TheClass")
        testEvaluation(codeElements, brClassFile, brMethod) {
            val clazzInstance = clazz.getDeclaredConstructor().newInstance()
            val clazzMethod = clazz.getMethod("tryCatchFinallyTest", classOf[String])
            clazzMethod.invoke(clazzInstance, "test")
        }
    }

    it should "generate the right stackmap for instance methods with long arguments" in {
        val LongType = ObjectType.Long
        val ExceptionType = ObjectType.Exception

        val longValue = INVOKEVIRTUAL(LongType, "longValue", MethodDescriptor.JustReturnsLong)

        // instancemethod long -> void
        val codeElements = Array[CodeElement[AnyRef]](
            LabeledGOTO(Symbol("EP1")),
            LabelElement(PCLabel(0)),
            /*DEAD*/ LoadLong(Long.MinValue),
            /*DEAD*/ LabelElement(PCLabel(3)),
            /*DEAD*/ LSTORE_1,
            /*DEAD*/ LabelElement(PCLabel(4)),
            /*DEAD*/ ACONST_NULL,
            /*DEAD*/ LabelElement(PCLabel(5)),
            /*DEAD*/ POP, ACONST_NULL, // INVOKEDYNAMIC run(run.coroutines.Benchmark)),
            /*DEAD*/ LabelElement(PCLabel(10)),
            /*DEAD*/ ACONST_NULL, //  get static run.coroutines.Benchmark.DATA : java.util.List[],
            /*DEAD*/ LabelElement(PCLabel(13)),
            /*DEAD*/ ASTORE(8),
            /*DEAD*/ ASTORE(7),
            /*DEAD*/ LLOAD_1,
            /*DEAD*/ POP2, ACONST_NULL, // INVOKEDYNAMIC enter(long)
            /*DEAD*/ POP, // effekt.Effekt{ void push(effekt.runtime.Frame) }),
            /*DEAD*/ ALOAD(7),
            /*DEAD*/ ALOAD(8),
            /*DEAD*/ TRY(Symbol("EHlambda$findMaxCoroutines$2$entrypoint$1")),
            /*DEAD*/ POP, POP, ACONST_NULL, // INVOKESTATIC(run.coroutines.Coroutine{ run.coroutines.Coroutine call(run.coroutines.CoroutineBody,java.lang.Object) }),
            /*DEAD*/ RETURN,
            /*DEAD*/ TRYEND(Symbol("EHlambda$findMaxCoroutines$2$entrypoint$1")),
            /*DEAD*/ CATCH(Symbol("EHlambda$findMaxCoroutines$2$entrypoint$1"), position = 0, Some(ExceptionType)),
            /*DEAD*/ POP, //INVOKESTATIC(effekt.Effekt{ void onThrow(java.lang.Throwable) }),
            /*DEAD*/ RETURN,
            Symbol("EP1"),
            LLOAD_0,
            LSTORE_1,
            ACONST_NULL, // INVOKESTATIC(effekt.Effekt{ java.lang.Object result() }),
            NOP, // CHECKCAST(run.coroutines.Coroutine),
            LabelElement(PCLabel(16)),
            ASTORE_3,
            LabelElement(PCLabel(17)),
            ALOAD_3,
            LabelElement(PCLabel(18)),
            POP, LCONST_0, INVOKESTATIC("java/lang/Long", false, "valueOf", "(J)Ljava/lang/Long;"), // INVOKEINTERFACE(run.coroutines.Coroutine{ java.lang.Object value() }),
            LabelElement(PCLabel(23)),
            CHECKCAST(LongType),
            LabelElement(PCLabel(26)),
            LCONST_0, // longValue,
            LabelElement(PCLabel(29)),
            LSTORE(4),
            LabelElement(PCLabel(31)),
            LLOAD(4),
            LabelElement(PCLabel(33)),
            LLOAD_1,
            LabelElement(PCLabel(34)),
            LCMP,
            LabelElement(PCLabel(35)),
            LabeledIFLE(PCLabel(41)),
            LabelElement(PCLabel(38)),
            LLOAD(4),
            LabelElement(PCLabel(40)),
            LSTORE_1,
            LabelElement(PCLabel(41)),
            ALOAD_3,
            LabelElement(PCLabel(42)),
            ASTORE(7),
            LLOAD_1,
            ALOAD_3,
            POP, POP2, ACONST_NULL, // INVOKEDYNAMIC(enter(long,run.coroutines.Coroutine)),
            POP, // INVOKESTATIC(effekt.Effekt{ void push(effekt.runtime.Frame) }),
            ALOAD(7),
            TRY(Symbol("EHlambda$findMaxCoroutines$2$entrypoint$2")),
            POP, ICONST_1, // INVOKEINTERFACE(run.coroutines.Coroutine{ boolean resume() }),
            RETURN,
            TRYEND(Symbol("EHlambda$findMaxCoroutines$2$entrypoint$2")),
            CATCH(Symbol("EHlambda$findMaxCoroutines$2$entrypoint$2"), 1, Some(ExceptionType)),
            POP, // INVOKESTATIC(effekt.Effekt{ void onThrow(java.lang.Throwable) }),
            RETURN,
            /*DEAD*/ Symbol("EP2"),
            /*DEAD*/ LLOAD_0,
            /*DEAD*/ ALOAD_2,
            /*DEAD*/ ASTORE_3,
            /*DEAD*/ LSTORE_1,
            /*DEAD*/ ICONST_1, // INVOKESTATIC(effekt.Effekt{ int resultI() }),
            /*DEAD*/ LabelElement(PCLabel(47)),
            /*DEAD*/ LabeledIFNE(PCLabel(17)),
            /*DEAD*/ LabelElement(PCLabel(50)),
            /*DEAD*/ LLOAD_1,
            /*DEAD*/ LabelElement(PCLabel(51)),
            /*DEAD*/ longValue,
            /*DEAD*/ LabelElement(PCLabel(54)),
            /*DEAD*/ DUP,
            /*DEAD*/ POP, // INVOKESTATIC(effekt.Effekt{ void returnWith(java.lang.Object) }),
            /*DEAD*/ RETURN,
            /*DEAD*/ LabelElement(PCLabel(55))
        )

        val c = CODE(codeElements)
        val classBuilder = CLASS(
            version = org.opalj.bi.Java8Version,
            accessModifiers = PUBLIC,
            thisType = "CodeAttributeBuilderTestClass",
            methods = METHODS(
                METHOD(
                    PUBLIC, "<init>", "()V",
                    CODE[AnyRef](
                        LINENUMBER(0),
                        ALOAD_0,
                        LINENUMBER(1),
                        INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                        Symbol("return"),
                        LINENUMBER(2),
                        RETURN
                    ) MAXSTACK 2 MAXLOCALS 3
                ),
                METHOD(PUBLIC.STATIC, "takeLong", "(J)V", c)
            )
        )
        val (brClassFile, _) = classBuilder.toBR()
        val brMethod = brClassFile.findMethod("takeLong").head
        val daClassFile = ba.toDA(brClassFile)
        // org.opalj.io.writeAndOpen(
        //    daClassFile.toXHTML(Some("CodeAttributeBuilderTest.scala")), "CodeAttributeBuilderTestTheClass", ".class.html"
        // )
        val rawClassFile = Assembler(daClassFile)

        val loader = new InMemoryClassLoader(
            Map("CodeAttributeBuilderTestClass" -> rawClassFile), this.getClass.getClassLoader
        )
        val clazz = loader.loadClass("CodeAttributeBuilderTestClass")
        testEvaluation(codeElements, brClassFile, brMethod) {
            val clazzInstance = clazz.getDeclaredConstructor().newInstance()
            val clazzMethod = clazz.getMethod("takeLong", classOf[Long])
            clazzMethod.invoke(clazzInstance, java.lang.Long.valueOf(1L))
        }
    }

    it should "generate the right stackmap for handlers with returns" in {
        val thisName = "TestClass"
        val PrintStreamType = ObjectType("java/io/PrintStream")

        val otherMethod = METHOD(PUBLIC.STATIC, "otherMethod", "()Z", CODE[AnyRef](
            ICONST_0,
            IRETURN
        ))
        val returnWith = METHOD(PUBLIC.STATIC, "returnWith", "(I)V", CODE[AnyRef](
            RETURN
        ))

        val codeElements = Array[CodeElement[AnyRef]](
            TRY(Symbol("eh1")),
            ALOAD_0,
            ASTORE_1,
            GETSTATIC("java/lang/System", "out", PrintStreamType.toJVMTypeName),
            LoadString("bar"),
            INVOKEVIRTUAL(PrintStreamType, "println", JustTakes(ObjectType.String)),
            ALOAD_1,
            POP,
            INVOKESTATIC(thisName, false, "otherMethod", "()Z"),
            POP,
            TRYEND(Symbol("eh1")),
            LabeledGOTO(Symbol("handler")),
            CATCH(Symbol("eh1"), 0),
            ASTORE_1,
            ICONST_0,
            DUP,
            INVOKESTATIC(thisName, false, "returnWith", "(I)V"),
            IRETURN,
            Symbol("handler"),
            ICONST_1,
            DUP,
            INVOKESTATIC(thisName, false, "returnWith", "(I)V"),
            IRETURN
        )
        val stackMapMethod = METHOD(PUBLIC, "stackMap", "()Z", CODE[AnyRef](codeElements))

        val classBuilder = CLASS(
            version = org.opalj.bi.Java8Version,
            accessModifiers = PUBLIC,
            thisType = thisName,
            methods = METHODS(
                METHOD(
                    PUBLIC, "<init>", "()V",
                    CODE[AnyRef](
                        LINENUMBER(0),
                        ALOAD_0,
                        LINENUMBER(1),
                        INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                        Symbol("return"),
                        LINENUMBER(2),
                        RETURN
                    ) MAXSTACK 2 MAXLOCALS 3
                ),
                stackMapMethod,
                otherMethod,
                returnWith
            )
        )
        val (brClassFile, _) = classBuilder.toBR()
        val brMethod = brClassFile.findMethod("stackMap").head
        val daClassFile = ba.toDA(brClassFile)
        //        org.opalj.io.writeAndOpen(
        //            daClassFile.toXHTML(
        //                Some("CodeAttributeBuilderTest.scala")
        //            ),
        //            "TheClass",
        //            ".class.html"
        //        )
        val rawClassFile = Assembler(daClassFile)

        val loader = new InMemoryClassLoader(
            Map(thisName -> rawClassFile), this.getClass.getClassLoader
        )
        val clazz = loader.loadClass(thisName)
        testEvaluation(codeElements, brClassFile, brMethod) {
            val clazzInstance = clazz.getDeclaredConstructor().newInstance()
            val clazzMethod = clazz.getMethod("stackMap")
            clazzMethod.invoke(clazzInstance)
        }
    }

    it should "not remove live code after simple conditional branch instructions" in {
        import ObjectType.{Object => OObject}
        import ObjectType.{RuntimeException => ORuntimeException}
        val codeElements = Array[CodeElement[AnyRef]](
            LabelElement(0),
            TRY(Symbol("eh")),
            ACONST_NULL,
            LabelElement(1),
            ACONST_NULL,
            LabelElement(2),
            INVOKEVIRTUAL(OObject, "equals", MethodDescriptor(OObject, BooleanType)),
            LabelElement(5),
            ICONST_0,
            LabelElement(6),
            IRETURN,
            TRYEND(Symbol("eh")),
            CATCH(Symbol("eh"), 1, Some(ORuntimeException)),
            LabelElement(7),
            POP,
            LabelElement(8),
            ICONST_1,
            LabelElement(9),
            ICONST_2,
            LabelElement(10),
            LabeledIFNE(16),
            LabelElement(13),
            POP,
            ICONST_0,
            IRETURN,
            LabelElement(16),
            IRETURN
        )
        val c = CODE[AnyRef](codeElements)
        val expectedInstructions = Array(
            /* 00 */ ACONST_NULL,
            /* 01 */ ACONST_NULL,
            /* 02 */ INVOKEVIRTUAL(OObject, "equals", MethodDescriptor(OObject, BooleanType)),
            /* 03 */ null,
            /* 04 */ null,
            /* 05 */ ICONST_0,
            /* 06 */ IRETURN,
            /* 07 */ POP,
            /* 08 */ ICONST_1,
            /* 09 */ ICONST_2,
            /* 10 */ IFNE(6),
            /* 11 */ null,
            /* 12 */ null,
            /* 13 */ POP,
            /* 14 */ ICONST_0,
            /* 15 */ IRETURN,
            /* 16 */ IRETURN
        )
        assert(c.instructions === expectedInstructions)
        assert(c.exceptionHandlers.head == ExceptionHandler(0, 7, 7, Some(ORuntimeException)))
    }

    it should "allow explicitly specified ExceptionHandlers that include the last PC" in {
        val code = br.Code(0, 0, Array(
            /* 0 */ GOTO(6),
            /* 1 */ null,
            /* 2 */ null,
            /* 3 */ POP,
            /* 4 */ ICONST_2,
            /* 5 */ IRETURN,
            /* 6 */ ICONST_0,
            /* 7 */ ICONST_1,
            /* 8 */ IADD,
            /* 9 */ IRETURN
        ),
            ArraySeq(ExceptionHandler(7, 10, 3, Some(ObjectType.RuntimeException))))
        val labeledCode = LabeledCode(code)
        val labeledInstructions = labeledCode.codeElements.toIndexedSeq
        assert(labeledInstructions(2) == CATCH(Symbol("eh0"), 0, Some(ObjectType.RuntimeException)))
        assert(labeledInstructions(11) == TRY(Symbol("eh0")))
        assert(labeledInstructions(18) == TRYEND(Symbol("eh0")))
    }

    it should "allow inline ExceptionHandlers that include the last PC" in {
        import ObjectType.{RuntimeException => ORuntimeException}
        import ObjectType.{Object => OObject}
        val codeElements = Array[CodeElement[AnyRef]](
            GOTO(Symbol("NORMAL_CF")), // => 0,1,2
            CATCH(Symbol("eh"), 1, Some(ORuntimeException)),
            POP, // => 3
            ICONST_2, // => 4
            IRETURN, // => 5
            Symbol("NORMAL_CF"),
            TRY(Symbol("eh")),
            ACONST_NULL, // => 6
            INVOKEVIRTUAL(OObject, "hashCode", MethodDescriptor.JustReturnsInteger), // => 7,8,9
            IRETURN, // => 10
            TRYEND(Symbol("eh"))
        )
        val code = CODE[AnyRef](codeElements)
        assert(code.exceptionHandlers.head == ExceptionHandler(6, 11, 3, Some(ORuntimeException)))
    }
}
