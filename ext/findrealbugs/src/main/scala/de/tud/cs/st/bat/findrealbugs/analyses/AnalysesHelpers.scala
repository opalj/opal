/* License (BSD Style License):
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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

package de.tud.cs.st
package bat
package findrealbugs
package analyses

import resolved._
import resolved.instructions._

/**
 * Helper methods common to FindRealBug's analyses.
 *
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
            method ← classFile.methods if method.body.isDefined
            FieldReadAccess(declaringClass,
                name, fieldType) ← method.body.get.instructions
        } yield {
            ((classFile, method), (declaringClass, name, fieldType))
        }
    }

    /**
     * Helper function to retrieve a line number for a certain program counter (pc). The
     * result is optional because the `lineNumberTable` may be unavailable as well, in
     * which case we cannot query the line number.
     *
     * @param code Code object to query line number from, e.g. `Method.body.get`.
     * @param pc instruction index to query line number for, e.g. from
     * `associateWithIndex()`.
     * @return The line number or None if it's unavailable.
     */
    def pcToOptionalLineNumber(code: Code, pc: PC): Option[Int] = {
        code.lineNumberTable.map(_.lookupLineNumber(pc)).getOrElse(None)
    }
}
