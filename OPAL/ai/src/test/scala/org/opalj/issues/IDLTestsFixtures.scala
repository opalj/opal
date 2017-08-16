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
package issues

import play.api.libs.json.JsObject
import play.api.libs.json.Json
import play.api.libs.json.JsNull

import org.opalj.collection.immutable.Chain
import org.opalj.collection.mutable.Locals
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_STATIC
import org.opalj.br.ArrayType
import org.opalj.br.ByteType
import org.opalj.br.ClassFile
import org.opalj.br.Code
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
        Json.obj("name" → name, "value" → value)
    }

    val simplePackageLocation = new PackageLocation(Option("foo"), null, "bar/baz")

    val attributes = IndexedSeq(CompactLineNumberTable(Array[Byte](0, 0, 10, 0, 0, 0, 0, 10)))

    val code = Code(0, 0, Array(new IFEQ(0)))

    val codeWithLineNumbers = Code(0, 0, Array(new IFEQ(0)), attributes = attributes)

    val simplePackageLocationIDL: JsObject = Json.obj(
        "location" → Json.obj("package" → "bar.baz"),
        "description" → "foo",
        "details" → Json.arr()
    )

    val classFileIDL: JsObject = Json.obj(
        "fqn" → "foo/Bar",
        "type" → Json.obj("ot" → "foo.Bar", "simpleName" → "Bar"),
        "accessFlags" → "public"
    )

    val methodReturnVoidNoParametersIDL: JsObject = Json.obj(
        "accessFlags" → "public",
        "name" → "test0p",
        "returnType" → Json.obj("vt" → "void"),
        "parameters" → Json.arr(),
        "signature" → "test0p()V",
        "firstLine" → JsNull
    )
    private[this] val methodTemplateReturnVoidNoParameters = {
        Method(ACC_PUBLIC.mask, "test0p", IndexedSeq.empty, VoidType, Seq(code))
    }

    private[this] val methodTemplateReturnIntOneParameter = {
        Method(ACC_PUBLIC.mask | ACC_STATIC.mask, "test1p", IndexedSeq(ObjectType("foo/Bar")), IntegerType)
    }

    val methodReturnIntTwoParametersIDL: JsObject = Json.obj(
        "accessFlags" → "public static",
        "name" → "test2p",
        "returnType" → Json.obj("bt" → "int"),
        "parameters" → Json.arr(
            Json.obj("at" → Json.obj("bt" → "byte"), "dimensions" → 2),
            Json.obj("ot" → "foo.Bar", "simpleName" → "Bar")
        ),
        "signature" → "test2p([[BLfoo/Bar;)I",
        "firstLine" → "8"
    )
    private[issues] val methodTemplateReturnIntTwoParameters = Method(
        ACC_PUBLIC.mask | ACC_STATIC.mask,
        "test2p",
        IndexedSeq(ArrayType(2, ByteType), ObjectType("foo/Bar")),
        IntegerType,
        Seq(codeWithLineNumbers)
    )

    // an arbitrary (actually invalid) class file
    val classFile = ClassFile(
        0,
        1,
        ACC_PUBLIC.mask,
        ObjectType("foo/Bar"),
        Option.empty,
        Nil,
        IndexedSeq(),
        IndexedSeq(
            methodTemplateReturnVoidNoParameters,
            methodTemplateReturnIntOneParameter,
            methodTemplateReturnIntTwoParameters
        ),
        Seq()
    )

    val methodReturnVoidNoParameters = classFile.methods(0)
    val methodReturnIntOneParameter = classFile.methods(1)
    val methodReturnIntTwoParameters = classFile.methods(2)

    val simpleOperands = new Operands(code, 0, Chain("foo"), null)

    val simpleOperandsIDL: JsObject = Json.obj(
        "type" → "SimpleConditionalBranchInstruction",
        "operator" → "== 0",
        "value" → "foo",
        "value2" → JsNull
    )

    val simpleLocalVariables = new LocalVariables(code, 0, Locals.empty)

    val simpleLocalVariablesIDL: JsObject = {
        Json.obj("type" → "LocalVariables", "values" → Json.arr())
    }

}
