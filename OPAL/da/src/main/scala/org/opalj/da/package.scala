/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

import scala.xml.Node

import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContexts

/**
 * Defines convenience methods related to reading in class files.
 *
 * @author Michael Eichberg
 */
package object da {

    type Constant_Pool_Index = ClassFileReader.Constant_Pool_Index
    type Constant_Pool = ClassFileReader.Constant_Pool

    type Interfaces = IndexedSeq[Constant_Pool_Index]
    type Methods = IndexedSeq[Method_Info]
    type Fields = IndexedSeq[Field_Info]

    type Attributes = Seq[Attribute]

    def asReferenceType(cpIndex: Int)(implicit cp: Constant_Pool): String = {
        val rt = cp(cpIndex).toString(cp)
        if (rt.charAt(0) == '[')
            parseFieldType(rt.substring(1))+"[]"
        else
            asJavaObjectType(rt);
    }

    def asObjectType(cpIndex: Int)(implicit cp: Constant_Pool): String = {
        asJavaObjectType(cp(cpIndex).toString(cp))
    }

    def asJavaObjectType(t: String): String = {
        t.replace('/', '.')
    }

    def parseReturnType(type_index: Int)(implicit cp: Constant_Pool): String = {
        parseReturnType(cp(type_index).toString)
    }

    def parseReturnType(rt: String): String = {
        if (rt.charAt(0) == 'V')
            "void"
        else
            parseFieldType(rt)
    }

    def parseFieldType(type_index: Int)(implicit cp: Constant_Pool): String = {
        parseFieldType(cp(type_index).toString)
    }

    def parseFieldType(descriptor: String): String = {
        (descriptor.charAt(0): @scala.annotation.switch) match {
            case 'B' ⇒ "byte"
            case 'C' ⇒ "char"
            case 'D' ⇒ "double"
            case 'F' ⇒ "float"
            case 'I' ⇒ "int"
            case 'J' ⇒ "long"
            case 'S' ⇒ "short"
            case 'Z' ⇒ "boolean"
            case 'L' ⇒ descriptor.substring(1, descriptor.length - 1).replace('/', '.')
            case '[' ⇒ parseFieldType(descriptor.substring(1))+"[]"
            case _   ⇒ throw new IllegalArgumentException(descriptor+" is not a valid field type descriptor")
        }
    }

    def parseMethodDescriptor(
        //definingTypeFQN: String,
        methodName: String,
        descriptor: String): String = {
        var index = 1 // we are not interested in the leading '('
        var parameterTypes: IndexedSeq[String] = IndexedSeq.empty
        while (descriptor.charAt(index) != ')') {
            val (ft, nextIndex) = parseParameterType(descriptor, index)
            parameterTypes = parameterTypes :+ ft
            index = nextIndex
        }
        val returnType =
            parseReturnType(descriptor.substring(index + 1))

        s"$returnType $methodName(${parameterTypes.mkString(", ")})"
    }

    private[this] def parseParameterType(md: String, startIndex: Int): (String, Int) = {
        val td = md.charAt(startIndex)
        (td: @scala.annotation.switch) match {
            case 'L' ⇒
                val endIndex = md.indexOf(';', startIndex + 1)
                ( // this is the return tuple
                    md.substring(startIndex + 1, endIndex).replace('/', '.'),
                    endIndex + 1
                )
            case '[' ⇒
                val (ft, index) = parseParameterType(md, startIndex + 1)
                ( // this is the return tuple
                    ft+"[]",
                    index
                )
            case _ ⇒
                ( // this is the return tuple
                    parseFieldType(td.toString),
                    startIndex + 1
                )
        }
    }

    def methodAccessFlagsToString(access_flags: Int): String =
        AccessFlags.toString(access_flags, AccessFlagsContexts.METHOD)

    def abbreviateFQN(definingTypeFQN: String, memberTypeFQN: String): Node = {
        val abbreviatedMemberTypeFQN =
            org.opalj.abbreviateFQN(definingTypeFQN, memberTypeFQN, '.')

        if (abbreviatedMemberTypeFQN == memberTypeFQN)
            <span class="fqn"> { memberTypeFQN } </span>
        else
            <span class="fqn tooltip">
                { abbreviatedMemberTypeFQN }<span>{ memberTypeFQN }</span>
            </span>
    }

}

