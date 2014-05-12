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
package frb
package analyses

import br._
import br.analyses._
import br.instructions._

/**
 * Helper methods common to FindRealBug's analyses.
 *
 * @author Peter Spieler
 * @author Roberts Kolosovs
 * @author Daniel Klauer
 * @author Ralf Mitschke
 */
object AnalysesHelpers {
    val HashtableType = ObjectType("java/util/Hashtable")
    val SystemType = ObjectType("java/lang/System")
    val RuntimeType = ObjectType("java/lang/Runtime")
    val NoArgsAndReturnObject = MethodDescriptor(IndexedSeq(), ObjectType.Object)

    /**
     * Used as return type of [[getReadFields]].
     */
    private type ReadFieldInfo = ((ClassFile, Method), (ObjectType, String, Type))

    /**
     * Returns all declared fields that are read by a method in the analyzed classes
     * as tuple: (read-context, field-info) =
     * ((ClassFile, Method), (declaringClass, name, fieldType)).
     *
     * @param classFiles List of class files to search for field accesses.
     * @return List of accessed fields, associated with the class file and method where
     * each access was found.
     */
    def getReadFields(classFiles: Traversable[ClassFile]): Traversable[ReadFieldInfo] = {
        for {
            classFile ← classFiles if !classFile.isInterfaceDeclaration
            method @ MethodWithBody(body) ← classFile.methods
            FieldReadAccess(declaringClass, name, fieldType) ← body.instructions
        } yield {
            ((classFile, method), (declaringClass, name, fieldType))
        }
    }

    val integerWrapper = ObjectType("java/lang/Integer")
    val floatWrapper = ObjectType("java/lang/Float")
    val doubleWrapper = ObjectType("java/lang/Double")
    val booleanWrapper = ObjectType("java/lang/Boolean")
    val characterWrapper = ObjectType("java/lang/Character")
    val byteWrapper = ObjectType("java/lang/Byte")
    val shortWrapper = ObjectType("java/lang/Short")
    val longWrapper = ObjectType("java/lang/Long")

    /**
     * Checks if a given object type is a standard java wrapper class for a java primitive.
     *
     * @param objectType The type of the object to be checked.
     * @return `true` if the type is a wrapper for Java primitive type, else `false`.
     */
    def isPrimitiveWrapper(objectType: ObjectType): Boolean = {
        (objectType == integerWrapper || objectType == floatWrapper ||
            objectType == doubleWrapper || objectType == booleanWrapper ||
            objectType == characterWrapper || objectType == byteWrapper ||
            objectType == shortWrapper || objectType == longWrapper)
    }

    /**
     * Accumulates a list of all annotation types with a certain name in the project.
     *
     * @param project The project to be searched.
     * @return A `Set` of all the annotation types.
     */
    def collectAnnotationTypes[Source](
        project: Project[Source],
        name: String): Set[ObjectType] = {
        (for {
            classFile ← project.classFiles
            if (classFile.isAnnotationDeclaration &&
                classFile.thisType.simpleName.equals(name))
        } yield {
            ObjectType(classFile.fqn)
        }).toSet
    }

    /**
     * Determines whether a class is annotated with one of the annotations in the given
     * list.
     *
     * @param classFile The class to check.
     * @param annotationTypes list of annotations to check for.
     * @return `true` if the class is annotated with one of the annotations; `false`
     * otherwise.
     */
    def isAnnotatedWith(
        classFile: ClassFile,
        annotationTypes: Set[ObjectType]): Boolean = {
        classFile.annotations.exists(annotation ⇒
            annotationTypes.exists(_ == annotation.annotationType)
        )
    }
}
