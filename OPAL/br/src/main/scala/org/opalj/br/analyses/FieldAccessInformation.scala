/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.Map
import scala.collection.mutable.AnyRefMap
import org.opalj.collection.mutable.UShortSet
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC

class FieldAccessInformation(
        val project:          SomeProject,
        val allReadAccesses:  Map[Field, Seq[(Method, PCs)]],
        val allWriteAccesses: Map[Field, Seq[(Method, PCs)]],
        val unresolved:       IndexedSeq[(Method, PCs)]
) {

    private[this] def accesses(
        accessInformation:  Map[Field, Seq[(Method, PCs)]],
        declaringClassType: ObjectType,
        fieldName:          String
    ): Seq[(Method, PCs)] = {
        for {
            (field, wa) ← accessInformation
            if project.classFile(field).thisType eq declaringClassType
            if field.name == fieldName
        } {
            return wa;
        }

        Seq.empty
    }

    def writeAccesses(declaringClassType: ObjectType, fieldName: String): Seq[(Method, PCs)] = {
        accesses(allWriteAccesses, declaringClassType, fieldName)
    }

    def readAccesses(declaringClassType: ObjectType, fieldName: String): Seq[(Method, PCs)] = {
        accesses(allReadAccesses, declaringClassType, fieldName)
    }

    def statistics: Map[String, Int] =
        Map(
            "field reads" → allReadAccesses.values.map(_.map(_._2.size).sum).sum,
            "field writes" → allWriteAccesses.values.map(_.map(_._2.size).sum).sum,
            "unresolved field accesses" → unresolved.map(_._2.size).sum
        )

}

