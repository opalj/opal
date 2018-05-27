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
package br
package analyses

import scala.collection.Map

/**
 * Stores the information where each field is read and written. If the project
 * is incomplete the results are also necessarily incomplete. Reflective and comparable
 * accesses are not considered.
 *
 * @author Michael Eichberg
 */
class FieldAccessInformation(
        val project:          SomeProject,
        val allReadAccesses:  Map[Field, Seq[(Method, PCs)]],
        val allWriteAccesses: Map[Field, Seq[(Method, PCs)]],
        val unresolved:       Vector[(Method, PCs)]
) {

    private[this] def accesses(
        accessInformation:  Map[Field, Seq[(Method, PCs)]],
        declaringClassType: ObjectType,
        fieldName:          String
    ): Seq[(Method, PCs)] = {
        // FIX We can also use a reference to a subclass to access a field in a supertype
        accessInformation.collectFirst {
            case (field, accesses) if field.name == fieldName &&
                (field.classFile.thisType eq declaringClassType) ⇒ accesses
        }.getOrElse(Seq.empty)
    }

    def writeAccesses(declaringClassType: ObjectType, fieldName: String): Seq[(Method, PCs)] = {
        accesses(allWriteAccesses, declaringClassType, fieldName)
    }

    def readAccesses(declaringClassType: ObjectType, fieldName: String): Seq[(Method, PCs)] = {
        accesses(allReadAccesses, declaringClassType, fieldName)
    }

    final def writeAccesses(field: Field): Seq[(Method, PCs)] = {
        allWriteAccesses.getOrElse(field, Seq.empty)
    }

    final def readAccesses(field: Field): Seq[(Method, PCs)] = {
        allReadAccesses.getOrElse(field, Seq.empty)
    }

    def isRead(field: Field): Boolean = {
        allReadAccesses.get(field) match {
            case Some(accesses) ⇒ accesses.nonEmpty
            case None           ⇒ false
        }
    }

    def isWritten(field: Field): Boolean = {
        allWriteAccesses.get(field) match {
            case Some(accesses) ⇒ accesses.nonEmpty
            case None           ⇒ false
        }
    }

    final def isAccessed(field: Field): Boolean = isRead(field) || isWritten(field)

    /**
     * Returns a new iterator to iterate over all field access locations.
     */
    def allAccesses(field: Field): Iterator[(Method, PCs)] = {
        readAccesses(field).toIterator ++ writeAccesses(field).toIterator
    }

    /**
     * Basic statistics about the number of field reads and writes.
     */
    def statistics: Map[String, Int] = {
        Map(
            "field reads" → allReadAccesses.values.map(_.map(_._2.size).sum).sum,
            "field writes" → allWriteAccesses.values.map(_.map(_._2.size).sum).sum,
            "unresolved field accesses" → unresolved.map(_._2.size).sum
        )
    }

}
