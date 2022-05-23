/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import scala.language.postfixOps

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner

import org.opalj.bc.Assembler
import org.opalj.br.instructions.ALOAD_0
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.RETURN
import org.opalj.util.InMemoryClassLoader

/**
 * Tests annotating instructions in the BytecodeAssembler DSL.
 *
 * @author Malte Limmeroth
 */
@RunWith(classOf[JUnitRunner])
class AnnotatedInstructionsTest extends AnyFlatSpec {

    {
        behavior of "Instructions annotated with Strings"

        val brClassTemplate: CLASS[(Map[org.opalj.br.PC, String], List[String])] = CLASS(
            accessModifiers = PUBLIC SUPER,
            thisType = "Test",
            methods = METHODS(
                METHOD(PUBLIC, "<init>", "()V", CODE(
                    Symbol("UnUsedLabel1"),
                    ALOAD_0 -> "MarkerAnnotation1",
                    Symbol("UnUsedLabel2"),
                    INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                    RETURN -> "MarkerAnnotation2"
                ))
            )
        )
        val (
            daClassFile,
            methodAnnotations: Map[br.Method, (Map[br.PC, String], List[String])]
            ) = brClassTemplate.toDA()
        val (pcAnnotations: List[Map[br.PC, String]], warnings) =
            methodAnnotations.values.unzip

        println(pcAnnotations)

        "[String Annotated Instructions] the class generation" should "have no warnings" in {
            assert(warnings.flatten.isEmpty)
        }

        "[String Annotated Instructions] the generated class" should "load correctly" in {
            val loader = new InMemoryClassLoader(
                Map("Test" -> Assembler(daClassFile)), this.getClass.getClassLoader
            )
            assert("Test" == loader.loadClass("Test").getSimpleName)
        }

        "[String Annotated Instructions] the method " should "have the correct annotations" in {
            assert(pcAnnotations.head(0) == "MarkerAnnotation1")
            assert(pcAnnotations.head(4) == "MarkerAnnotation2")
        }
    }

    {
        behavior of "Instructions annotated with Tuples"

        val (
            daClassFile,
            methodAnnotations: Map[br.Method, (Map[br.PC, (Symbol, String)], List[String])]
            ) =
            CLASS(
                accessModifiers = PUBLIC SUPER,
                thisType = "Test",
                methods = METHODS(
                    METHOD(PUBLIC, "<init>", "()V", CODE(
                        Symbol("UnUsedLabel1"),
                        ALOAD_0 -> ((Symbol("L1"), "MarkerAnnotation1")),
                        Symbol("UnUsedLabel2"),
                        INVOKESPECIAL("java/lang/Object", false, "<init>", "()V"),
                        RETURN -> ((Symbol("L2"), "MarkerAnnotation2"))
                    ))
                )
            ).toDA()
        val (pcAnnotations: List[Map[br.PC, (Symbol, String)]], warnings) =
            methodAnnotations.values.unzip

        "[Tuple Annotated Instructions] the class generation" should "have no warnings" in {
            assert(warnings.flatten.isEmpty)
        }

        "[Tuple Annotated Instructions] the generated class" should "load correctly" in {
            val loader = new InMemoryClassLoader(
                Map("Test" -> Assembler(daClassFile)), this.getClass.getClassLoader
            )
            assert("Test" == loader.loadClass("Test").getSimpleName)
        }

        "[Tuple Annotated Instructions] the method" should "have the correct annotations" in {
            assert(pcAnnotations.head(0) == ((Symbol("L1"), "MarkerAnnotation1")))
            assert(pcAnnotations.head(4) == ((Symbol("L2"), "MarkerAnnotation2")))
        }
    }
}
