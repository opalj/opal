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
package br
package reader

import scala.collection.concurrent.TrieMap

import org.opalj.br.instructions._

class BytecodeInstructionsCache {

    //
    // String caching
    //

    private[this] val methodNames = TrieMap.empty[String, String]
    def MethodName(name: String): String = {
        methodNames.getOrElseUpdate(name, name.intern)
    }

    private[this] val fieldNames = TrieMap.empty[String, String]
    def FieldName(name: String): String = {
        fieldNames.getOrElseUpdate(name, name.intern)
    }

    //
    // Branch offset based caching
    //

    private[this] val gotoInstructions = {
        val instructions = new Array[GOTO](2000)
        for (i ← (0 until instructions.size)) instructions(i) = new GOTO(i - 1000)
        instructions
    }
    def GOTO(branchtarget: Int): GOTO =
        if (branchtarget >= -1000 && branchtarget < 1000) gotoInstructions(branchtarget + 1000)
        else { new GOTO(branchtarget) }

    private[this] val ifnullInstructions = {
        val instructions = new Array[IFNULL](1000)
        for (i ← (0 until instructions.size)) instructions(i) = new IFNULL(i - 150)
        instructions
    }
    def IFNULL(branchtarget: Int): IFNULL =
        if (branchtarget >= -150 && branchtarget < 850) ifnullInstructions(branchtarget + 150)
        else { new IFNULL(branchtarget) }

    private[this] val ifnonnullInstructions = {
        val instructions = new Array[IFNONNULL](500)
        for (i ← (0 until instructions.size)) instructions(i) = new IFNONNULL(i - 150)
        instructions
    }
    def IFNONNULL(branchtarget: Int): IFNONNULL =
        if (branchtarget >= -150 && branchtarget < 350) ifnonnullInstructions(branchtarget + 150)
        else { new IFNONNULL(branchtarget) }

    private[this] val ifacmpeqInstructions = {
        val instructions = new Array[IF_ACMPEQ](1000)
        for (i ← (0 until instructions.size)) instructions(i) = new IF_ACMPEQ(i - 150)
        instructions
    }
    def IF_ACMPEQ(branchtarget: Int): IF_ACMPEQ =
        if (branchtarget >= -150 && branchtarget < 850) ifacmpeqInstructions(branchtarget + 150)
        else { new IF_ACMPEQ(branchtarget) }

    private[this] val ifacmpneInstructions = {
        val instructions = new Array[IF_ACMPNE](500)
        for (i ← (0 until instructions.size)) instructions(i) = new IF_ACMPNE(i - 150)
        instructions
    }
    def IF_ACMPNE(branchtarget: Int): IF_ACMPNE =
        if (branchtarget >= -150 && branchtarget < 350) ifacmpneInstructions(branchtarget + 150)
        else { new IF_ACMPNE(branchtarget) }

    private[this] val ifneInstructions = {
        val instructions = new Array[IFNE](256 + 512)
        for (i ← (0 until instructions.size)) instructions(i) = new IFNE(i - 256)
        instructions
    }
    def IFNE(branchtarget: Int): IFNE =
        if (branchtarget >= -256 && branchtarget < 512) ifneInstructions(branchtarget + 256)
        else { new IFNE(branchtarget) }

    private[this] val ifeqInstructions = {
        val instructions = new Array[IFEQ](256 + 512)
        for (i ← (0 until instructions.size)) instructions(i) = new IFEQ(i - 256)
        instructions
    }
    def IFEQ(branchtarget: Int): IFEQ =
        if (branchtarget >= -256 && branchtarget < 512) ifeqInstructions(branchtarget + 256)
        else { new IFEQ(branchtarget) }

    private[this] val ifltInstructions = {
        val instructions = new Array[IFLT](128 + 256)
        for (i ← (0 until instructions.size)) instructions(i) = new IFLT(i - 128)
        instructions
    }
    def IFLT(branchtarget: Int): IFLT =
        if (branchtarget >= -128 && branchtarget < 256) ifltInstructions(branchtarget + 128)
        else { new IFLT(branchtarget) }

    private[this] val ifgtInstructions = {
        val instructions = new Array[IFGT](128 + 256)
        for (i ← (0 until instructions.size)) instructions(i) = new IFGT(i - 128)
        instructions
    }
    def IFGT(branchtarget: Int): IFGT =
        if (branchtarget >= -128 && branchtarget < 256) ifgtInstructions(branchtarget + 128)
        else { new IFGT(branchtarget) }

    private[this] val ifleInstructions = {
        val instructions = new Array[IFLE](128 + 256)
        for (i ← (0 until instructions.size)) instructions(i) = new IFLE(i - 128)
        instructions
    }
    def IFLE(branchtarget: Int): IFLE =
        if (branchtarget >= -128 && branchtarget < 256) ifleInstructions(branchtarget + 128)
        else { new IFLE(branchtarget) }

    private[this] val ifgeInstructions = {
        val instructions = new Array[IFGE](128 + 256)
        for (i ← (0 until instructions.size)) instructions(i) = new IFGE(i - 128)
        instructions
    }
    def IFGE(branchtarget: Int): IFGE =
        if (branchtarget >= -128 && branchtarget < 256) ifgeInstructions(branchtarget + 128)
        else { new IFGE(branchtarget) }

    private[this] val ificmpneInstructions = {
        val instructions = new Array[IF_ICMPNE](256 + 512)
        for (i ← (0 until instructions.size)) instructions(i) = new IF_ICMPNE(i - 256)
        instructions
    }
    def IF_ICMPNE(branchtarget: Int): IF_ICMPNE =
        if (branchtarget >= -256 && branchtarget < 512) ificmpneInstructions(branchtarget + 256)
        else { new IF_ICMPNE(branchtarget) }

    private[this] val ificmpeqInstructions = {
        val instructions = new Array[IF_ICMPEQ](256 + 512)
        for (i ← (0 until instructions.size)) instructions(i) = new IF_ICMPEQ(i - 256)
        instructions
    }
    def IF_ICMPEQ(branchtarget: Int): IF_ICMPEQ =
        if (branchtarget >= -256 && branchtarget < 512) ificmpeqInstructions(branchtarget + 256)
        else { new IF_ICMPEQ(branchtarget) }

    private[this] val ificmpltInstructions = {
        val instructions = new Array[IF_ICMPLT](128 + 256)
        for (i ← (0 until instructions.size)) instructions(i) = new IF_ICMPLT(i - 128)
        instructions
    }
    def IF_ICMPLT(branchtarget: Int): IF_ICMPLT =
        if (branchtarget >= -128 && branchtarget < 256) ificmpltInstructions(branchtarget + 128)
        else { new IF_ICMPLT(branchtarget) }

    private[this] val ificmpgtInstructions = {
        val instructions = new Array[IF_ICMPGT](128 + 256)
        for (i ← (0 until instructions.size)) instructions(i) = new IF_ICMPGT(i - 128)
        instructions
    }
    def IF_ICMPGT(branchtarget: Int): IF_ICMPGT =
        if (branchtarget >= -128 && branchtarget < 256) ificmpgtInstructions(branchtarget + 128)
        else { new IF_ICMPGT(branchtarget) }

    private[this] val ificmpleInstructions = {
        val instructions = new Array[IF_ICMPLE](128 + 256)
        for (i ← (0 until instructions.size)) instructions(i) = new IF_ICMPLE(i - 128)
        instructions
    }
    def IF_ICMPLE(branchtarget: Int): IF_ICMPLE =
        if (branchtarget >= -128 && branchtarget < 256) ificmpleInstructions(branchtarget + 128)
        else { new IF_ICMPLE(branchtarget) }

    private[this] val ificmpgeInstructions = {
        val instructions = new Array[IF_ICMPGE](128 + 256)
        for (i ← (0 until instructions.size)) instructions(i) = new IF_ICMPGE(i - 128)
        instructions
    }
    def IF_ICMPGE(branchtarget: Int): IF_ICMPGE =
        if (branchtarget >= -128 && branchtarget < 256) ificmpgeInstructions(branchtarget + 128)
        else { new IF_ICMPGE(branchtarget) }

    //
    // "Value" based caching
    //

    private[this] val sipushInstructions = {
        val instructions = new Array[SIPUSH](20000)
        for (i ← (0 until instructions.size)) instructions(i) = new SIPUSH(i - 10000)
        instructions
    }
    def SIPUSH(value: Int): SIPUSH =
        if (value >= -10000 && value < 10000) sipushInstructions(value + 10000)
        else { new SIPUSH(value) }

    //
    // Local variable index based caching
    //

    private[this] val retInstructions = {
        val instructions = new Array[RET](32)
        for (i ← (0 until instructions.size)) instructions(i) = new RET(i)
        instructions
    }
    def RET(lvIndex: Int): RET =
        if (lvIndex < 32) retInstructions(lvIndex)
        else { new RET(lvIndex) }

    // References/Return Address...
    //
    private[this] val aloadInstructions = {
        val instructions = new Array[ALOAD](256)
        for (i ← (0 until instructions.size)) instructions(i) = new ALOAD(i)
        instructions
    }
    def ALOAD(lvIndex: Int): ALOAD =
        if (lvIndex < 256) aloadInstructions(lvIndex)
        else { new ALOAD(lvIndex) }

    private[this] val astoreInstructions = {
        val instructions = new Array[ASTORE](256)
        for (i ← (0 until instructions.size)) instructions(i) = new ASTORE(i)
        instructions
    }
    def ASTORE(lvIndex: Int): ASTORE =
        if (lvIndex < 256) astoreInstructions(lvIndex)
        else { new ASTORE(lvIndex) }

    // Integer...
    //
    private[this] val iloadInstructions = {
        val instructions = new Array[ILOAD](128)
        for (i ← (0 until instructions.size)) instructions(i) = new ILOAD(i)
        instructions
    }
    def ILOAD(lvIndex: Int): ILOAD =
        if (lvIndex < 128) iloadInstructions(lvIndex)
        else { new ILOAD(lvIndex) }

    private[this] val istoreInstructions = {
        val instructions = new Array[ISTORE](128)
        for (i ← (0 until instructions.size)) instructions(i) = new ISTORE(i)
        instructions
    }
    def ISTORE(lvIndex: Int): ISTORE =
        if (lvIndex < 128) istoreInstructions(lvIndex)
        else { new ISTORE(lvIndex) }

    // Long...
    //
    private[this] val lloadInstructions = {
        val instructions = new Array[LLOAD](48)
        for (i ← (0 until instructions.size)) instructions(i) = new LLOAD(i)
        instructions
    }
    def LLOAD(lvIndex: Int): LLOAD =
        if (lvIndex < 48) lloadInstructions(lvIndex)
        else { new LLOAD(lvIndex) }

    private[this] val lstoreInstructions = {
        val instructions = new Array[LSTORE](48)
        for (i ← (0 until instructions.size)) instructions(i) = new LSTORE(i)
        instructions
    }
    def LSTORE(lvIndex: Int): LSTORE =
        if (lvIndex < 48) lstoreInstructions(lvIndex)
        else { new LSTORE(lvIndex) }

    // Float...
    //
    private[this] val floadInstructions = {
        val instructions = new Array[FLOAD](256)
        for (i ← (0 until instructions.size)) instructions(i) = new FLOAD(i)
        instructions
    }
    def FLOAD(lvIndex: Int): FLOAD =
        if (lvIndex < 256) floadInstructions(lvIndex)
        else { new FLOAD(lvIndex) }

    private[this] val fstoreInstructions = {
        val instructions = new Array[FSTORE](256)
        for (i ← (0 until instructions.size)) instructions(i) = new FSTORE(i)
        instructions
    }
    def FSTORE(lvIndex: Int): FSTORE =
        if (lvIndex < 256) fstoreInstructions(lvIndex)
        else { new FSTORE(lvIndex) }

    // Double...
    //
    private[this] val dloadInstructions = {
        val instructions = new Array[DLOAD](256)
        for (i ← (0 until instructions.size)) instructions(i) = new DLOAD(i)
        instructions
    }
    def DLOAD(lvIndex: Int): DLOAD =
        if (lvIndex < 256) dloadInstructions(lvIndex)
        else { new DLOAD(lvIndex) }

    private[this] val dstoreInstructions = {
        val instructions = new Array[DSTORE](256)
        for (i ← (0 until instructions.size)) instructions(i) = new DSTORE(i)
        instructions
    }
    def DSTORE(lvIndex: Int): DSTORE =
        if (lvIndex < 256) dstoreInstructions(lvIndex)
        else { new DSTORE(lvIndex) }

    //
    // ReferenceType based caching
    //

    private[this] val newInstructions = TrieMap.empty[ObjectType, NEW]
    def NEW(objectType: ObjectType) =
        newInstructions.getOrElseUpdate(objectType, new NEW(objectType))

    private[this] val checkcastInstructions = TrieMap.empty[ReferenceType, CHECKCAST]
    def CHECKCAST(referenceType: ReferenceType) =
        checkcastInstructions.getOrElseUpdate(referenceType, new CHECKCAST(referenceType))

    private[this] val instanceOfInstructions = TrieMap.empty[ReferenceType, INSTANCEOF]
    def INSTANCEOF(referenceType: ReferenceType) =
        instanceOfInstructions.getOrElseUpdate(referenceType, new INSTANCEOF(referenceType))

    private[this] val anewarrayInstructions = TrieMap.empty[ReferenceType, ANEWARRAY]
    def ANEWARRAY(referenceType: ReferenceType) =
        anewarrayInstructions.getOrElseUpdate(referenceType, new ANEWARRAY(referenceType))

}

