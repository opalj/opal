/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package issues

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.JsNull

import org.opalj.collection.mutable.Locals
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_STATIC
import org.opalj.br.ArrayType
import org.opalj.br.ByteType
import org.opalj.br.ClassFile
import org.opalj.br.Code
import org.opalj.br.Attributes
import org.opalj.br.Methods
import org.opalj.br.NoAttributes
import org.opalj.br.NoInterfaces
import org.opalj.br.NoFieldTemplates
import org.opalj.br.FieldTypes
import org.opalj.br.NoFieldTypes
import org.opalj.br.CompactLineNumberTable
import org.opalj.br.IntegerType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.VoidType
import org.opalj.br.instructions.IFEQ

/**
 * Commonly used helper methods and definitions.
 *
 * @author Lukas Berg
 * @author Michael Eichberg
 */
object IDLTestsFixtures {

    private[issues] def toIDL(relevance: Relevance): JsObject = {
        relevanceToIDL(relevance.name, relevance.value)
    }

    private[issues] def relevanceToIDL(name: String, value: Int): JsObject = {
        Json.obj("name" -> name, "value" -> value)
    }

    val simplePackageLocation = new PackageLocation(Option("foo"), null, "bar/baz")

    val attributes = Attributes(CompactLineNumberTable(Array[Byte](0, 0, 10, 0, 0, 0, 0, 10)))

    val code = Code(0, 0, Array(new IFEQ(0)))

    val codeWithLineNumbers = Code(0, 0, Array(new IFEQ(0)), attributes = attributes)

    val simplePackageLocationIDL: JsObject = Json.obj(
        "location" -> Json.obj("package" -> "bar.baz"),
        "description" -> "foo",
        "details" -> Json.arr()
    )

    val classFileIDL: JsObject = Json.obj(
        "fqn" -> "foo/Bar",
        "type" -> Json.obj("ot" -> "foo.Bar", "simpleName" -> "Bar"),
        "accessFlags" -> "public"
    )

    val methodReturnVoidNoParametersIDL: JsObject = Json.obj(
        "accessFlags" -> "public",
        "name" -> "test0p",
        "returnType" -> Json.obj("vt" -> "void"),
        "parameters" -> Json.arr(),
        "signature" -> "test0p()V",
        "firstLine" -> JsNull
    )
    private[this] val methodTemplateReturnVoidNoParameters = {
        Method(ACC_PUBLIC.mask, "test0p", NoFieldTypes, VoidType, Attributes(code))
    }

    private[this] val methodTemplateReturnIntOneParameter = {
        Method(ACC_PUBLIC.mask | ACC_STATIC.mask, "test1p", FieldTypes(ObjectType("foo/Bar")), IntegerType)
    }

    val methodReturnIntTwoParametersIDL: JsObject = Json.obj(
        "accessFlags" -> "public static",
        "name" -> "test2p",
        "returnType" -> Json.obj("bt" -> "int"),
        "parameters" -> Json.arr(
            Json.obj("at" -> Json.obj("bt" -> "byte"), "dimensions" -> 2),
            Json.obj("ot" -> "foo.Bar", "simpleName" -> "Bar")
        ),
        "signature" -> "test2p([[BLfoo/Bar;)I",
        "firstLine" -> "8"
    )
    private[issues] val methodTemplateReturnIntTwoParameters = Method(
        ACC_PUBLIC.mask | ACC_STATIC.mask,
        "test2p",
        FieldTypes(ArrayType(2, ByteType), ObjectType("foo/Bar")),
        IntegerType,
        Attributes(codeWithLineNumbers)
    )

    // an arbitrary (actually invalid) class file
    val classFile = ClassFile(
        0,
        1,
        ACC_PUBLIC.mask,
        ObjectType("foo/Bar"),
        Option.empty,
        NoInterfaces,
        NoFieldTemplates,
        Methods(
            methodTemplateReturnVoidNoParameters,
            methodTemplateReturnIntOneParameter,
            methodTemplateReturnIntTwoParameters
        ),
        NoAttributes
    )

    val methodReturnVoidNoParameters = classFile.methods(0)
    val methodReturnIntOneParameter = classFile.methods(1)
    val methodReturnIntTwoParameters = classFile.methods(2)

    val simpleOperands = new Operands(code, 0, List("foo"), null)

    val simpleOperandsIDL: JsObject = Json.obj(
        "type" -> "SimpleConditionalBranchInstruction",
        "operator" -> "== 0",
        "value" -> "foo",
        "value2" -> JsNull
    )

    val simpleLocalVariables = new LocalVariables(code, 0, Locals.empty)

    val simpleLocalVariablesIDL: JsObject = {
        Json.obj("type" -> "LocalVariables", "values" -> Json.arr())
    }

}
