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

import org.opalj.br.Method
import org.opalj.br.Field
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.FieldType
import org.opalj.br.analyses.MethodInfo
import org.opalj.br.analyses.Project

/**
 * The location where a specific feature was found. In general, a feature query should always use
 * a [[LocationsContainer]] to manage the identified locations.
 *
 * @tparam S The kind of the source. E.g., `java.net.URL`.
 *
 * @author Michael Eichberg
 */
sealed abstract class Location[S] {

    /**
     * The source location.
     */
    def source: Option[S]

}

case class PackageLocation[S](
        override val source: Option[S],
        packageName:         String
) extends Location[S] {

    override def toString: String = {
        source match {
            case Some(source) ⇒ s"$packageName\n$source"
            case None         ⇒ packageName
        }
    }
}
object PackageLocation {

    def apply[S](source: S, packageName: String): PackageLocation[S] = {
        new PackageLocation[S](Some(source), packageName)
    }

    def apply[S](packageName: String): PackageLocation[S] = {
        new PackageLocation[S](None, packageName)
    }
}

case class ClassFileLocation[S](
        override val source: Option[S],
        classFileFQN:        String
) extends Location[S] {

    override def toString: String = {
        source match {
            case Some(source) ⇒ s"$classFileFQN\n$source"
            case None         ⇒ classFileFQN
        }
    }

}

object ClassFileLocation {

    def apply[S](classFile: ClassFile): ClassFileLocation[S] = {
        new ClassFileLocation[S](None, classFile.thisType.toJava)
    }

    def apply[S](objectType: ObjectType): ClassFileLocation[S] = {
        new ClassFileLocation[S](None, objectType.toJava)
    }

    def apply[S](source: S, classFile: ClassFile): ClassFileLocation[S] = {
        new ClassFileLocation[S](Some(source), classFile.thisType.toJava)
    }

    def apply[S](project: Project[S], objectType: ObjectType): ClassFileLocation[S] = {
        new ClassFileLocation[S](project.source(objectType), objectType.toJava)
    }

    final def apply[S](project: Project[S], classFile: ClassFile): ClassFileLocation[S] = {
        apply(project, classFile.thisType)
    }

}

case class FieldLocation[S](
        classFileLocation: ClassFileLocation[S],
        fieldName:         String,
        fieldType:         FieldType
) extends Location[S] {

    override def source: Option[S] = classFileLocation.source

    def classFileFQN: String = classFileLocation.classFileFQN

    override def toString: String = {

        val s = s"${classFileLocation.classFileFQN}{ /*field*/ $fieldName : ${fieldType.toJava} }"
        val source = classFileLocation.source
        if (source.isDefined)
            s + s"\n${source.get}"
        else
            s
    }
}

object FieldLocation {

    def apply[S](classFileLocation: ClassFileLocation[S], field: Field): FieldLocation[S] = {
        new FieldLocation[S](classFileLocation, field.name, field.fieldType)
    }
}

case class MethodLocation[S](
        classFileLocation: ClassFileLocation[S],
        methodSignature:   String
) extends Location[S] {

    override def source: Option[S] = classFileLocation.source

    def classFileFQN: String = classFileLocation.classFileFQN

    override def toString: String = {

        val s = s"${classFileLocation.classFileFQN}{ /*method*/ $methodSignature }"
        val source = classFileLocation.source
        if (source.isDefined)
            s + s"\n${source.get}"
        else
            s
    }

}
object MethodLocation {

    def apply[S](source: S, method: Method): MethodLocation[S] = {
        val cf = method.classFile
        val md = method.descriptor.toJava(method.name)
        new MethodLocation(ClassFileLocation(source, cf), md)
    }

    def apply[S](classFileLocation: ClassFileLocation[S], method: Method): MethodLocation[S] = {
        new MethodLocation(classFileLocation, method.descriptor.toJava(method.name))
    }

    final def apply[S](methodInfo: MethodInfo[S]): MethodLocation[S] = {
        MethodLocation(methodInfo.source, methodInfo.method)
    }

}

case class InstructionLocation[S](methodLocation: MethodLocation[S], pc: Int) extends Location[S] {

    override def source: Option[S] = methodLocation.source

    def classFileFQN: String = methodLocation.classFileFQN

    def methodSignature: String = methodLocation.methodSignature

    override def toString: String = {
        val classFileLocation = methodLocation.classFileLocation
        val source = classFileLocation.source
        val s = s"${classFileLocation.classFileFQN}{ $methodSignature { $pc } }"
        if (source.isDefined)
            s + s"\n${source.get}"
        else
            s
    }
}
object InstructionLocation {

    def apply[S](source: S, method: Method, pc: Int): InstructionLocation[S] = {
        val classFileLocation = ClassFileLocation(source, method.classFile)
        val methodLocation = MethodLocation(classFileLocation, method.name + method.descriptor)
        new InstructionLocation(methodLocation, pc)
    }

}
