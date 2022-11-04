/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import scala.language.postfixOps
import scala.reflect.runtime.universe._
import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

import java.io.ByteArrayInputStream
import org.opalj.util.InMemoryClassLoader
import org.opalj.bc.Assembler
import org.opalj.bi._
import org.opalj.br.MethodDescriptor
import org.opalj.br.instructions._
import org.opalj.br.reader.Java8Framework.{ClassFile => J8ClassFile}
import org.opalj.br.MethodAttributeBuilder
import org.opalj.br.ObjectType
import org.opalj.br.IntegerType

import scala.collection.immutable.ArraySeq

/**
 * Tests the properties of a method in a class build with the BytecodeAssembler DSL. The class is
 * build, assembled as a [[org.opalj.da.ClassFile]] and read again as a [[org.opalj.br.ClassFile]].
 * It is also loaded, instantiated and the methods are executed with the JVM.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class MethodBuilderTest extends AnyFlatSpec {
    behavior of "the MethodBuilder"

    val (simpleMethodClass, _) =
        CLASS(
            accessModifiers = PUBLIC SUPER,
            thisType = "SimpleMethodClass",
            methods = METHODS(
                METHOD(
                    FINAL.SYNTHETIC.PUBLIC, "testMethod", "(Ljava/lang/String;)Ljava/lang/String;",
                    CODE(ACONST_NULL, ARETURN),
                    ArraySeq[MethodAttributeBuilder](EXCEPTIONS("java/lang/Exception"), br.Deprecated)
                )
            )
        ).toDA()

    val rawClassFile = Assembler(simpleMethodClass)

    "the generated method 'SimpleMethodClass.testMethod'" should "execute correctly" in {
        val loader = new InMemoryClassLoader(
            Map("SimpleMethodClass" -> rawClassFile),
            this.getClass.getClassLoader
        )

        val simpleMethodClazz = loader.loadClass("SimpleMethodClass")
        val simpleMethodInstance = simpleMethodClazz.getDeclaredConstructor().newInstance()
        val mirror = runtimeMirror(loader).reflect(simpleMethodInstance)
        val method = mirror.symbol.typeSignature.member(TermName("testMethod")).asMethod

        assert(mirror.reflectMethod(method)("test") == null)
    }

    val brClassFile = J8ClassFile(() => new java.io.ByteArrayInputStream(rawClassFile)).head

    val testMethod = brClassFile.methods.find { m =>
        val expectedMethodDescritor = MethodDescriptor("(Ljava/lang/String;)Ljava/lang/String;")
        m.name == "testMethod" && m.descriptor == expectedMethodDescritor
    }

    it should "have the correct signature: (Ljava/lang/String;)Ljava/lang/String;" in {
        assert(testMethod.isDefined)
    }

    it should "be public final synthetic" in {
        assert(
            testMethod.get.accessFlags == (ACC_PUBLIC.mask | ACC_FINAL.mask | ACC_SYNTHETIC.mask)
        )
    }

    it should "have the Exception attribute set: 'java/lang/Exception" in {
        val attribute = testMethod.get.attributes.collect { case e: br.ExceptionTable => e }
        assert(attribute.head.exceptions.head.fqn == "java/lang/Exception")
    }

    it should "be Deprecated" in {
        assert(testMethod.get.isDeprecated)
    }

    "maxLocals of method SimpleMethodClass.testMethod" should "be set automatically to: 2" in {
        assert(testMethod.get.body.get.maxLocals == 2)
    }

    "maxStack of method SimpleMethodClass.testMethod" should "be set automatically to: 1" in {
        assert(testMethod.get.body.get.maxStack == 1)
    }

    val (attributeMethodClass, _) =
        CLASS(
            accessModifiers = PUBLIC,
            thisType = "AttributeMethodClass",
            methods = METHODS(
                METHOD(
                    PUBLIC, "<init>", "()V",
                    CODE(
                        LINENUMBER(0),
                        ALOAD_0,
                        LINENUMBER(1),
                        INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                        Symbol("return"),
                        LINENUMBER(2),
                        RETURN
                    ) MAXSTACK 2 MAXLOCALS 3
                ),
                METHOD(
                    PUBLIC, "tryCatchFinallyTest", "(I)I",
                    CODE(
                        ICONST_1,
                        ISTORE_2,
                        TRY(Symbol("Try1")),
                        TRY(Symbol("FinallyTry2")),
                        TRY(Symbol("LastPCTry3")),
                        ILOAD_1,
                        IFGE(Symbol("tryEnd")),
                        NEW("java/lang/Exception"),
                        DUP,
                        INVOKESPECIAL("java/lang/Exception", false, "<init>", "()V"),
                        ATHROW,
                        Symbol("tryEnd"),
                        TRYEND(Symbol("Try1")),
                        GOTO(Symbol("finally")),
                        CATCH(Symbol("Try1"), 0, "java/lang/Exception"),
                        POP,
                        ICONST_0,
                        ISTORE_2,
                        TRYEND(Symbol("FinallyTry2")),
                        GOTO(Symbol("finally")),
                        CATCH(Symbol("FinallyTry2"), 1),
                        CATCH(Symbol("LastPCTry3"), 2),
                        POP,
                        Symbol("finally"),
                        ILOAD_1,
                        IFLE(Symbol("return")),
                        ICONST_2,
                        ISTORE_2,
                        Symbol("return"),
                        ILOAD_2,
                        IRETURN,
                        TRYEND(Symbol("LastPCTry3"))
                    ) MAXLOCALS 3
                )
            )
        ).toDA()

    val rawAttributeCF = Assembler(attributeMethodClass)
    val attributeBrClassFile = J8ClassFile(() => new ByteArrayInputStream(rawAttributeCF)).head

    val attributeTestMethod = attributeBrClassFile.methods.find { m =>
        m.name == "<init>" && m.descriptor == MethodDescriptor("()V")
    }.get

    val loader = new InMemoryClassLoader(
        Map("AttributeMethodClass" -> rawAttributeCF),
        this.getClass.getClassLoader
    )

    "the generated method 'AttributeMethodClass.<init>'" should "have 'maxStack' set to: 2" in {
        assert(attributeTestMethod.body.get.maxStack == 2)
    }

    it should "have 'maxLocals' set to: 3" in {
        assert(attributeTestMethod.body.get.maxLocals == 3)
    }

    it should "have the expected LineNumberTable" in {
        val lineNumberTable = attributeTestMethod.body.get.attributes.collect {
            case l: br.LineNumberTable => l
        }.head
        assert(lineNumberTable.lookupLineNumber(0).get == 0)
        assert(lineNumberTable.lookupLineNumber(1).get == 1)
        assert(lineNumberTable.lookupLineNumber(4).get == 2)
    }

    "the generated method `tryCatchFinallyTest`" should "have the correct exceptionTable set" in {
        val exceptionTable = attributeBrClassFile.methods.find {
            m => m.name == "tryCatchFinallyTest"
        }.get.body.get.exceptionHandlers
        assert(
            exceptionTable(0) ==
                br.ExceptionHandler(2, 14, 17, Some(br.ObjectType("java/lang/Exception")))

        )
        assert(exceptionTable(1) == br.ExceptionHandler(2, 20, 23, None))
        assert(exceptionTable(2) == br.ExceptionHandler(2, 32, 23, None))
    }

    "the generated method `tryCatchFinallyTest`" should "execute as expected" in {
        try {
            val attributeMethodClass = loader.loadClass("AttributeMethodClass")
            val attributeTestInstance = attributeMethodClass.getDeclaredConstructor().newInstance()
            val mirror = runtimeMirror(loader).reflect(attributeTestInstance)
            val method = mirror.symbol.typeSignature.member(TermName("tryCatchFinallyTest")).asMethod
            assert(mirror.reflectMethod(method)(-1) == 0)
            assert(mirror.reflectMethod(method)(0) == 1)
            assert(mirror.reflectMethod(method)(1) == 2)
        } catch {
            case t: Throwable =>
                info(
                    attributeBrClassFile.findMethod("tryCatchFinallyTest").head.toJava
                )
                org.opalj.io.writeAndOpen(
                    attributeMethodClass.toXHTML(Some("AttributeMethodClass.scala")),
                    "AttributeMethodClass",
                    ".class.html"
                )
                info(t.getLocalizedMessage)
                fail(t)
        }
    }

    "removing dead code related to TRY/CATCH" should "work correctly with \"standard\" TRY/CATCH" in {
        val c = CODE(
            LabeledGOTO(Symbol("b")),
            TRY(Symbol("try")),
            NOP,
            TRYEND(Symbol("try")),
            CATCH(Symbol("try"), 0),
            ATHROW,
            Symbol("b"),
            ICONST_0,
            IRETURN
        )
        assert(c.instructions(0) == GOTO(3))
        assert(c.instructions(3) == ICONST_0)
        assert(c.exceptionHandlers.isEmpty)
    }

    it should "work correctly with TRY/CATCH when the CATCH precedes the TRY" in {
        val c = CODE(
            LabeledGOTO(Symbol("b")),
            CATCH(Symbol("try"), 0),
            ATHROW,
            TRY(Symbol("try")),
            ICONST_0,
            TRYEND(Symbol("try")),
            Symbol("b"),
            ICONST_0,
            IRETURN
        )
        assert(c.instructions(0) == GOTO(3))
        assert(c.instructions(3) == ICONST_0)
        assert(c.exceptionHandlers.isEmpty)
    }

    it should "work correctly when the CATCH is between the TRY and the TRYEND" in {
        // this case is typically found in cases where we have synchronized methods
        val c = CODE(
            LabeledGOTO(Symbol("b")),
            TRY(Symbol("try")),
            CATCH(Symbol("try"), 0),
            ICONST_1,
            IRETURN,
            TRYEND(Symbol("try")),
            Symbol("b"),
            ICONST_0,
            IRETURN
        )
        assert(c.exceptionHandlers.isEmpty)
        assert(
            c.instructions(0) == GOTO(3),
            c.instructions.mkString("expected: instructions(0) == GOTO(3); found:\n", "\n", "\n")
        )
        assert(c.instructions(3) == ICONST_0)

    }

    it should "correctly remove useless TRY/CATCHs if no exceptions are thrown" in {
        val c = CODE(
            LabeledGOTO(Symbol("b")),
            CATCH(Symbol("try"), 0),
            ATHROW,
            TRY(Symbol("try")),
            ICONST_0,
            Symbol("b"),
            ICONST_0,
            TRYEND(Symbol("try")),
            IRETURN
        )
        assert(c.instructions(0) == GOTO(3))
        assert(c.instructions(3) == ICONST_0)
        assert(c.exceptionHandlers.isEmpty)
    }

    "dead code removal" should "not remove live code" in {
        // The following test is inspired by a regression posted by Jonathan in relation
        // to a PR fixing exception handler related issues.
        val c = CODE(
            LabeledGOTO(Symbol("EP1")),
            LabelElement(PCLabel(0)),
            ALOAD_0,
            LabelElement(PCLabel(1)),
            NOP, // INVOKESTATIC(effekt.Effekt{ void beforeEffect() })),
            POP, ICONST_0, // INVOKEINTERFACE(run.parsers.CharParsers{ boolean alternative() })),
            ICONST_0, // INVOKESTATIC(effekt.Effekt{ boolean isEffectful() })),
            LabeledIFEQ(Symbol("EPResume1")),
            POP,
            ALOAD_0,
            POP, ACONST_NULL, // INVOKEDYNAMIC(BootstrapMethod(InvokeStaticMethodHandle(ObjectType(java/lang/invoke/LambdaMetafactory),false,metafactory,MethodDescriptor((java.lang.invoke.MethodHandles$Lookup, java.lang.String, java.lang.invoke.MethodType, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle, java.lang.invoke.MethodType): java.lang.invoke.CallSite)),Vector(MethodDescriptor((): void), InvokeStaticMethodHandle(ObjectType(run/parsers/CharParsers),false,minimal$entrypoint$1,MethodDescriptor((run.parsers.CharParsers): void)), MethodDescriptor((): void))), target=effekt.Frame enter(run.parsers.CharParsers)),
            POP, //INVOKESTATIC(effekt.Effekt{ void push(effekt.Frame) }),
            RETURN,
            LabelElement(Symbol("EP1")),
            ALOAD_0,
            ASTORE_0,
            ICONST_1, // INVOKESTATIC(effekt.Effekt{ int resultI() }),
            LabelElement(Symbol("EPResume1")),
            LabelElement(PCLabel(6)),
            LabeledIFEQ(PCLabel(19)),
            LabelElement(PCLabel(9)),
            ALOAD_0,
            LabelElement(PCLabel(10)),
            NOP, // INVOKESTATIC(effekt.Effekt{ void beforeEffect() }),
            POP, // INVOKEINTERFACE(run.parsers.CharParsers{ int digit() }),
            ICONST_1, //INVOKESTATIC(effekt.Effekt{ boolean isEffectful() }),
            LabeledIFEQ(Symbol("EPResume2")),
            POP,
            ALOAD_0,
            POP, ACONST_NULL, //INVOKEDYNAMIC(BootstrapMethod(InvokeStaticMethodHandle(ObjectType(java/lang/invoke/LambdaMetafactory),false,metafactory,MethodDescriptor((java.lang.invoke.MethodHandles$Lookup, java.lang.String, java.lang.invoke.MethodType, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle, java.lang.invoke.MethodType): java.lang.invoke.CallSite)),Vector(MethodDescriptor((): void), InvokeStaticMethodHandle(ObjectType(run/parsers/CharParsers),false,minimal$entrypoint$2,MethodDescriptor((run.parsers.CharParsers): void)), MethodDescriptor((): void))), target=effekt.Frame enter(run.parsers.CharParsers)),
            POP, // INVOKESTATIC(effekt.Effekt{ void push(effekt.Frame) }),
            RETURN,
            /*DEAD*/ LabelElement(Symbol("EP2")),
            /*DEAD*/ ALOAD_0,
            /*DEAD*/ ASTORE_0,
            /*DEAD*/ ICONST_1, // INVOKESTATIC(effekt.Effekt{ int resultI() }),
            LabelElement(Symbol("EPResume2")),
            LabelElement(PCLabel(15)),
            POP,
            LabelElement(PCLabel(16)),
            LabeledGOTO(PCLabel(0)),
            LabelElement(PCLabel(19)),
            BIPUSH(42),
            LabelElement(PCLabel(21)),
            DUP,
            POP, // INVOKESTATIC(effekt.Effekt{ void returnWith(int) }),
            RETURN,
            LabelElement(PCLabel(22))
        )

        assert(c.instructions.size == 45)
    }

    it should "aggressively remove useless try markers if no exceptions are thrown" in {
        val SystemType = ObjectType("java/lang/System")
        val PrintStreamType = ObjectType("java/io/PrintStream")
        val ExceptionType = ObjectType("java/lang/Exception")

        val c = CODE(
            LabeledGOTO(Symbol("EP1")),
            LabelElement(PCLabel(0)),
            /*DEAD*/ ICONST_0,
            LabelElement(PCLabel(1)),
            /*DEAD*/ ISTORE_0,
            /*DEAD*/ TRY(Symbol("eh0")),
            LabelElement(PCLabel(2)),
            /*DEAD*/ ACONST_NULL, // INVOKEDYNAMIC ...
            /*DEAD*/ POP, // INVOKESTATIC(effekt.Effekt{ void push(effekt.Frame) }),
            /*DEAD*/ TRY(Symbol("EHeffectOp2$entrypoint$1")),
            /*DEAD*/ ICONST_1, // INVOKESTATIC(run.SimpleExceptions{ int effectOp1() }),
            /*DEAD*/ RETURN,
            /*DEAD*/ TRYEND(Symbol("EHeffectOp2$entrypoint$1")),
            /*DEAD*/ CATCH(Symbol("EHeffectOp2$entrypoint$1"), 0, Some(ExceptionType)),
            /*DEAD*/ POP, //INVOKESTATIC(effekt.Effekt{ void onThrow(java.lang.Throwable) }),
            /*DEAD*/ RETURN,
            LabelElement(Symbol("EP1")),
            ICONST_1, // INVOKESTATIC(effekt.Effekt{ int resultI() }),
            LabelElement(PCLabel(5)),
            ISTORE_0,
            /*DEAD*/ TRYEND(Symbol("eh0")),
            LabeledGOTO(PCLabel(21)),
            /*DEAD*/ CATCH(Symbol("eh0"), 0, Some(ExceptionType)),
            LabelElement(PCLabel(9)),
            /*DEAD*/ ASTORE_1,
            LabelElement(PCLabel(10)),
            /*DEAD*/ GETSTATIC(SystemType, "out", PrintStreamType),
            LabelElement(PCLabel(13)),
            /*DEAD*/ BIPUSH(10), // loadstring "got it"
            LabelElement(PCLabel(15)),
            /*DEAD*/ INVOKEVIRTUAL(PrintStreamType, "println", MethodDescriptor.JustTakes(IntegerType)),
            LabelElement(PCLabel(18)),
            /*DEAD*/ BIPUSH(42),
            LabelElement(PCLabel(20)),
            /*DEAD*/ ISTORE_0,
            LabelElement(PCLabel(21)),
            ILOAD_0,
            LabelElement(PCLabel(22)),
            DUP,
            POP, // INVOKESTATIC(effekt.Effekt{ void returnWith(int) }),
            RETURN,
            LabelElement(PCLabel(23))
        )

        assert(c.instructions.size == 12)
    }

    it should "not remove live code in nested exception handlers" in {
        val SystemType = ObjectType("java/lang/System")
        val PrintStreamType = ObjectType("java/io/PrintStream")
        val ExceptionType = ObjectType("java/lang/Exception")

        val c = CODE(
            LabeledGOTO(Symbol("EP1")),
            LabelElement(PCLabel(0)),
            /*DEAD*/ ICONST_0,
            LabelElement(PCLabel(1)),
            /*DEAD*/ ISTORE_0,
            TRY(Symbol("eh0")),
            LabelElement(PCLabel(2)),
            /*DEAD*/ ACONST_NULL, // INVOKEDYNAMIC ...
            /*DEAD*/ POP, // INVOKESTATIC(effekt.Effekt{ void push(effekt.Frame) }),
            /*DEAD*/ TRY(Symbol("EHeffectOp2$entrypoint$1")),
            /*DEAD*/ ICONST_1, // INVOKESTATIC(run.SimpleExceptions{ int effectOp1() }),
            /*DEAD*/ RETURN,
            /*DEAD*/ TRYEND(Symbol("EHeffectOp2$entrypoint$1")),
            /*DEAD*/ CATCH(Symbol("EHeffectOp2$entrypoint$1"), 0, Some(ExceptionType)),
            /*DEAD*/ POP, //INVOKESTATIC(effekt.Effekt{ void onThrow(java.lang.Throwable) }),
            /*DEAD*/ RETURN,
            LabelElement(Symbol("EP1")),
            ICONST_1, // INVOKESTATIC(effekt.Effekt{ int resultI() }),
            ICONST_2,
            IDIV, // we need an instruction which potentially throws an exception...
            LabelElement(PCLabel(5)),
            ISTORE_0,
            TRYEND(Symbol("eh0")),
            LabeledGOTO(PCLabel(21)),
            CATCH(Symbol("eh0"), 0, Some(ExceptionType)),
            LabelElement(PCLabel(9)),
            ASTORE_1,
            LabelElement(PCLabel(10)),
            GETSTATIC(SystemType, "out", PrintStreamType),
            LabelElement(PCLabel(13)),
            BIPUSH(10), // loadstring "got it"
            LabelElement(PCLabel(15)),
            INVOKEVIRTUAL(PrintStreamType, "println", MethodDescriptor.JustTakes(IntegerType)),
            LabelElement(PCLabel(18)),
            BIPUSH(42),
            LabelElement(PCLabel(20)),
            ISTORE_0,
            LabelElement(PCLabel(21)),
            ILOAD_0,
            LabelElement(PCLabel(22)),
            DUP,
            POP, // INVOKESTATIC(effekt.Effekt{ void returnWith(int) }),
            RETURN,
            LabelElement(PCLabel(23))
        )

        assert(c.instructions.size == 26)
    }

}
