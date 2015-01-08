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
import org.opalj.collection.mutable.UShortSet
import scala.collection.mutable.AnyRefMap
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC

/**
 * Stores the information which field is accessed where.
 *
 * @author Michael Eichberg
 */
object FieldAccessInformationAnalysis {

    def doAnalyze(
        project: SomeProject,
        isInterrupted: () ⇒ Boolean): FieldAccessInformation = {

        val classHierarchy = project.classHierarchy

        val allReadAccesses = new ConcurrentHashMap[Field, List[(Method, PCs)]]()
        val allWriteAccesses = new ConcurrentHashMap[Field, List[(Method, PCs)]]()
        val allUnresolved = new ConcurrentLinkedQueue[(Method, PCs)]()

        project.parForeachMethodWithBody(isInterrupted) { e ⇒
            val (_, _ /*classFile*/ , method) = e

            val readAccesses = AnyRefMap.empty[Field, UShortSet]
            val writeAccesses = AnyRefMap.empty[Field, UShortSet]
            val unresolved = UShortSet.empty
            method.body.get.foreach { (pc, instruction) ⇒
                instruction.opcode match {

                    case GETFIELD.opcode | GETSTATIC.opcode ⇒
                        val FieldReadAccess(declaringClassType, fieldName, fieldType) = instruction
                        classHierarchy.resolveFieldReference(declaringClassType, fieldName, fieldType, project) match {
                            case Some(field) ⇒
                                readAccesses.update(
                                    field,
                                    pc +≈: readAccesses.getOrElse(field, UShortSet.empty)
                                )
                            case None ⇒
                                pc +≈: unresolved
                        }

                    case PUTFIELD.opcode | PUTSTATIC.opcode ⇒
                        val FieldWriteAccess(declaringClassType, fieldName, fieldType) = instruction
                        classHierarchy.resolveFieldReference(declaringClassType, fieldName, fieldType, project) match {
                            case Some(field) ⇒
                                writeAccesses.update(
                                    field,
                                    pc +≈: writeAccesses.getOrElse(field, UShortSet.empty)
                                )
                            case None ⇒
                                pc +≈: unresolved
                        }

                    case _ ⇒ /*nothing to do*/
                }
            }

            // merge with the global store
            readAccesses.foreach { e ⇒
                val (field, pcs) = e
                field.synchronized {
                    allReadAccesses.put(
                        field, (method, pcs) :: allReadAccesses.getOrDefault(field, Nil)
                    )
                }
            }
            writeAccesses.foreach { e ⇒
                val (field, pcs) = e
                field.synchronized {
                    allWriteAccesses.put(
                        field, (method, pcs) :: allWriteAccesses.getOrDefault(field, Nil)
                    )
                }
            }
            allUnresolved.add((method, unresolved))
        }

        import scala.collection.JavaConverters._
        FieldAccessInformation(
            AnyRefMap.empty ++ allReadAccesses.asScala,
            AnyRefMap.empty ++ allWriteAccesses.asScala,
            Seq.empty ++ allUnresolved.asScala
        )
    }
}

case class FieldAccessInformation(
        readAccesses: Map[Field, Seq[(Method, PCs)]],
        writeAccesses: Map[Field, Seq[(Method, PCs)]],
        unresolved: Seq[(Method, PCs)]) {

    def statistics: Map[String, Int] =
        Map(
            "readAccesses" -> readAccesses.values.map(_.map(_._2.size).sum).sum,
            "writeAccesses" -> writeAccesses.values.map(_.map(_._2.size).sum).sum,
            "unresolved" -> unresolved.map(_._2.size).sum
        )

}

