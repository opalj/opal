/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

import org.junit.runner.RunWith
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

import play.api.libs.json.JsNull
import play.api.libs.json.Json

/**
 * Tests toIDL method of IssueLocation
 *
 * @author Lukas Berg
 */
@RunWith(classOf[JUnitRunner])
class IssueLocationIDLTest extends AnyFlatSpec with Matchers {

    import IDLTestsFixtures._

    behavior of "the toIDL method"

    it should "return a valid issue description for a basic PackageLocation" in {
        simplePackageLocation.toIDL should be(simplePackageLocationIDL)
    }

    it should "return a valid issue description for a PackageLocation with details" in {
        val packageLocation = new PackageLocation(
            Option("bar"), null, "baz", Seq(simpleOperands, simpleLocalVariables)
        )

        packageLocation.toIDL should be(Json.obj(
            "description" -> "bar",
            "location" -> Json.obj("package" -> "baz"),
            "details" -> Json.arr(simpleOperandsIDL, simpleLocalVariablesIDL)
        ))
    }

    it should "return a valid issue description for a basic ClassLocation" in {
        val classLocation = new ClassLocation(Option("baz"), null, classFile)

        classLocation.toIDL should be(Json.obj(
            "description" -> "baz",
            "location" -> Json.obj(
                "package" -> "foo",
                "class" -> classFileIDL
            ),
            "details" -> Json.arr()
        ))
    }

    it should "return a valid issue description for a ClassLocation with details" in {
        val classLocation = new ClassLocation(
            Option("baz"), null, classFile, Seq(simpleOperands, simpleLocalVariables)
        )

        classLocation.toIDL should be(Json.obj(
            "description" -> "baz",
            "location" -> Json.obj(
                "package" -> "foo",
                "class" -> classFileIDL
            ),
            "details" -> Json.arr(simpleOperandsIDL, simpleLocalVariablesIDL)
        ))
    }

    it should "return a valid issue description for a method with no parameters and no return value" in {
        val methodLocation = new MethodLocation(Option("baz"), null, methodReturnVoidNoParameters)

        methodLocation.toIDL should be(Json.obj(
            "description" -> "baz",
            "location" -> Json.obj(
                "package" -> "foo",
                "class" -> classFileIDL,
                "method" -> methodReturnVoidNoParametersIDL
            ),
            "details" -> Json.arr()
        ))
    }

    it should "return a valid issue description for a method with two int parameters and which returns int values" in {
        val methodLocation = new MethodLocation(Option("baz"), null, methodReturnIntTwoParameters)

        methodLocation.toIDL should be(Json.obj(
            "description" -> "baz",
            "location" -> Json.obj(
                "package" -> "foo",
                "class" -> classFileIDL,
                "method" -> methodReturnIntTwoParametersIDL
            ),
            "details" -> Json.arr()
        ))
    }

    it should "return a valid issue description for a method which returns ints and declares one parameter" in {
        val methodLocation = new MethodLocation(Option("baz"), null, methodReturnIntOneParameter)

        methodLocation.toIDL should be(Json.obj(
            "description" -> "baz",
            "location" -> Json.obj(
                "package" -> "foo",
                "class" -> classFileIDL,
                "method" -> Json.obj(
                    "accessFlags" -> "public static",
                    "name" -> "test1p",
                    "returnType" -> Json.obj(
                        "bt" -> "int"
                    ),
                    "parameters" -> Json.arr(Json.obj(
                        "ot" -> "foo.Bar",
                        "simpleName" -> "Bar"
                    )),
                    "signature" -> "test1p(Lfoo/Bar;)I",
                    "firstLine" -> JsNull
                )
            ),
            "details" -> Json.arr()
        ))
    }

    it should "return a valid issue description for a method with two int parameters and which returns int values and which has further details" in {
        val methodLocation = new MethodLocation(
            Option("baz"),
            null,
            methodReturnIntTwoParameters,
            Seq(simpleOperands, simpleLocalVariables)
        )

        methodLocation.toIDL should be(Json.obj(
            "description" -> "baz",
            "location" -> Json.obj(
                "package" -> "foo",
                "class" -> classFileIDL,
                "method" -> methodReturnIntTwoParametersIDL
            ),
            "details" -> Json.arr(simpleOperandsIDL, simpleLocalVariablesIDL)
        ))
    }

    it should "return a valid issue description for an InstructionLocation in a method without parameters which returns nothing" in {
        val instructionLocation = new InstructionLocation(
            Option("baz"), null, methodReturnVoidNoParameters, 42
        )

        instructionLocation.toIDL should be(Json.obj(
            "description" -> "baz",
            "location" -> Json.obj(
                "package" -> "foo",
                "class" -> classFileIDL,
                "method" -> methodReturnVoidNoParametersIDL,
                "instruction" -> Json.obj("pc" -> 42)
            ),
            "details" -> Json.arr()
        ))
    }

    it should "return a valid issue description for InstructionLocation with int return and 2 parameters" in {
        val instructionLocation = new InstructionLocation(
            Some("baz"), null, methodReturnIntTwoParameters, 42
        )

        instructionLocation.toIDL should be(Json.obj(
            "description" -> "baz",
            "location" -> Json.obj(
                "package" -> "foo",
                "class" -> classFileIDL,
                "method" -> methodReturnIntTwoParametersIDL,
                "instruction" -> Json.obj("pc" -> 42, "line" -> 10)
            ),
            "details" -> Json.arr()
        ))
    }

    it should "return a valid issue description for InstructionLocation with int return, 2 parameters and details" in {
        val instructionLocation = new InstructionLocation(
            Some("baz"),
            null,
            methodReturnIntTwoParameters,
            42,
            Seq(simpleOperands, simpleLocalVariables)
        )

        instructionLocation.toIDL should be(Json.obj(
            "description" -> "baz",
            "location" -> Json.obj(
                "package" -> "foo",
                "class" -> classFileIDL,
                "method" -> methodReturnIntTwoParametersIDL,
                "instruction" -> Json.obj("pc" -> 42, "line" -> 10)
            ),
            "details" -> Json.arr(simpleOperandsIDL, simpleLocalVariablesIDL)
        ))
    }
}
