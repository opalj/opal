/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import scala.language.postfixOps

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

import java.io.ByteArrayInputStream

import scala.reflect.runtime.universe._

import org.opalj.bc.Assembler
import org.opalj.br.Method
import org.opalj.br.instructions._
import org.opalj.br.reader.Java8Framework.{ClassFile => ClassFileReader}
import org.opalj.util.InMemoryClassLoader

/**
 * Tests the branchoffset calculation of LabeledBranchInstructions in the BytecodeAssembler DSL
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class JumpLabelsTest extends AnyFlatSpec {

    val methodTemplate =
        METHOD(PUBLIC, "returnInt", "(I)I", CODE(
            GOTO(Symbol("IsZero_?")),
            Symbol("Else"),
            ILOAD_1,
            IRETURN,
            Symbol("IsTwo_?"),
            ILOAD_1,
            ICONST_2,
            IF_ICMPNE(Symbol("Else")),
            ICONST_2,
            IRETURN,
            Symbol("IsOne_?"),
            ILOAD_1,
            ICONST_1,
            IF_ICMPNE(Symbol("IsTwo_?")),
            ICONST_1,
            IRETURN,
            Symbol("IsZero_?"),
            ILOAD_1,
            IFNE(Symbol("IsOne_?")),
            ICONST_0,
            IRETURN
        ))

    val (daJava5ClassFile, _) =
        CLASS(
            version = bi.Java5Version,
            accessModifiers = PUBLIC SUPER,
            thisType = "TestJumpJava5",
            methods = METHODS(methodTemplate)
        ).toDA()
    val rawJava5ClassFile = Assembler(daJava5ClassFile)
    val brJava5ClassFile = ClassFileReader(() => new ByteArrayInputStream(rawJava5ClassFile)).head

    // We basically test that we compute the (correct) stack map table attribute
    val (daJava8ClassFile, _) =
        CLASS(
            version = bi.Java8Version,
            accessModifiers = PUBLIC SUPER,
            thisType = "TestJumpJava8",
            methods = METHODS(methodTemplate)
        ).toDA()
    val rawJava8ClassFile = Assembler(daJava8ClassFile)
    val brJava8ClassFile = ClassFileReader(() => new ByteArrayInputStream(rawJava8ClassFile)).head

    "the method returnInt" should "execute as expected" in {
        val classes = Map("TestJumpJava5" -> rawJava5ClassFile, "TestJumpJava8" -> rawJava8ClassFile)
        val loader = new InMemoryClassLoader(classes, this.getClass.getClassLoader)
        def testClass(clazz: Class[_]): Unit = {
            val testJumpInstance = clazz.getDeclaredConstructor().newInstance()

            val mirror = runtimeMirror(loader).reflect(testJumpInstance)
            val method = mirror.symbol.typeSignature.member(TermName("returnInt")).asMethod

            assert(mirror.reflectMethod(method)(0) == 0)
            assert(mirror.reflectMethod(method)(1) == 1)
            assert(mirror.reflectMethod(method)(2) == 2)
            assert(mirror.reflectMethod(method)(10) == 10)
        }

        testClass(loader.loadClass("TestJumpJava5"))
        testClass(loader.loadClass("TestJumpJava8"))
    }

    "each BranchInstruction" should "have the correct branch offset" in {
        def testMethods(methods: Seq[Method]): Unit = {
            val instructions = methods.find(_.name == "returnInt").get.body.get.instructions
            assert(instructions(0).asInstanceOf[GOTO].branchoffset == 19)
            assert(instructions(7).asInstanceOf[IF_ICMPNE].branchoffset == -4)
            assert(instructions(14).asInstanceOf[IF_ICMPNE].branchoffset == -9)
            assert(instructions(20).asInstanceOf[IFNE].branchoffset == -8)
        }

        testMethods(brJava5ClassFile.methods)
        testMethods(brJava8ClassFile.methods)
    }
}
