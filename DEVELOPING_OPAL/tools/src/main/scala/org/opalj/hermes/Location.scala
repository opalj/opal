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
package hermes

import org.opalj.br.PC
import org.opalj.br.Method
import org.opalj.br.ClassFile

/**
 * The location where a specific feature was found.
 *
 * @tparam S The kind of the source. E.g., `java.net.URL`.
 *
 * @author Michael Eichberg
 */
sealed trait Location[S] {

    /**
     * The source location.
     */
    def source: S

}

case class ClassFileLocation[S](
        override val source: S,
        classFileFQN:        String
) extends Location[S] {

    override def toString: String = s"$classFileFQN\n$source"

}
object ClassFileLocation {

    def apply[S](source: S, classFile: ClassFile): ClassFileLocation[S] = {
        new ClassFileLocation[S](source, classFile.thisType.toJava)
    }

}

case class FieldLocation[S](
        classFileLocation: ClassFileLocation[S],
        fieldName:         String
) extends Location[S] {

    override def source: S = classFileLocation.source

    def classFileFQN: String = classFileLocation.classFileFQN

    override def toString: String = {
        val source = classFileLocation.source
        s"${classFileLocation.classFileFQN}{ /*field*/ $fieldName }\n$source"
    }
}

case class MethodLocation[S](
        classFileLocation: ClassFileLocation[S],
        methodSignature:   String
) extends Location[S] {

    override def source: S = classFileLocation.source

    def classFileFQN: String = classFileLocation.classFileFQN

    override def toString: String = {
        val source = classFileLocation.source
        s"${classFileLocation.classFileFQN}{ /*method*/ $methodSignature }\n$source"
    }

}
object MethodLocation {

    def apply[S](source: S, classFile: ClassFile, method: Method): MethodLocation[S] = {
        val md = method.descriptor.toJava(method.name)
        new MethodLocation(ClassFileLocation(source, classFile), md)
    }

    def apply[S](classFileLocation: ClassFileLocation[S], method: Method): MethodLocation[S] = {
        new MethodLocation(classFileLocation, method.descriptor.toJava(method.name))
    }

}

case class InstructionLocation[S](methodLocation: MethodLocation[S], pc: PC) extends Location[S] {

    override def source: S = methodLocation.source

    def classFileFQN: String = methodLocation.classFileFQN

    def methodSignature: String = methodLocation.methodSignature

    override def toString: String = {
        val classFileLocation = methodLocation.classFileLocation
        val source = classFileLocation.source
        s"${classFileLocation.classFileFQN}{ $methodSignature { $pc } }\n$source"
    }
}
object InstructionLocation {

    def apply[S](
        source:    S,
        classFile: ClassFile,
        method:    Method,
        pc:        PC
    ): InstructionLocation[S] = {
        val classFileLocation = ClassFileLocation(source, classFile)
        val methodLocation = MethodLocation(classFileLocation, method.name + method.descriptor)
        new InstructionLocation(methodLocation, pc)
    }

}
