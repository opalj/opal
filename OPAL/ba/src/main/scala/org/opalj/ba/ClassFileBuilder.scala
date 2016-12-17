/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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

import org.opalj.ba.br2da.ClassConvert
import org.opalj.bi.ACC_INTERFACE
import org.opalj.bi.ACC_PUBLIC
import org.opalj.br.{ClassFile ⇒ BRClassFile}
import org.opalj.br.Code
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.instructions.ALOAD_0
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.RETURN
import org.opalj.collection.immutable.UShortPair
import org.opalj.da.{ClassFile ⇒ DAClassFile}

/**
 * ClassFileBuilder for the parameters specified after Fields or Methods have been added.
 */
case class ClassFileBuilder(
        private val classFile: BRClassFile,
        annotations:           Map[String, Map[PC, Any]] = Map.empty
) {
    /**
     * Defines the SourceFile attribute.
     */
    def SourceFile(sourceFile: String): ClassFileBuilder = {
        this.copy(
            classFile = classFile.copy(
                attributes = classFile.attributes :+ org.opalj.br.SourceFile(sourceFile)
            )
        )
    }

    /**
     * Defines the minorVersion and majorVersion. The default values are the unchanged values.
     */
    def Version(
        minorVersion: Int = classFile.version.minor,
        majorVersion: Int = classFile.version.major
    ): ClassFileBuilder = {
        this.copy(classFile = classFile.copy(version = UShortPair(minorVersion, majorVersion)))
    }

    /**
     * Returns the build [[org.opalj.br.ClassFile]]. For classes without the `Interface` access flag
     * set, a default constructor will be generated if no constructor was defined.
     */
    def buildBRClassFile: BRClassFile = {
        if ((classFile.accessFlags & ACC_INTERFACE.mask) != 0
            || classFile.methods.exists(_.name == "<init>")) {
            classFile
        } else {
            val superclassType = classFile.superclassType.get
            val newMethods = classFile.methods :+ Method.defaultConstructor(superclassType)
            classFile.copy(methods = newMethods)
        }

    }

    /**
     * Returns the build [[org.opalj.da.ClassFile]].
     *
     * @see [[buildBRClassFile]]
     */
    def buildDAClassFile: DAClassFile = buildBRClassFile.assembleToDA

}

object ClassFileBuilder {
    final val defaultMinorVersion = 0
    final val defaultMajorVersion = 50
}