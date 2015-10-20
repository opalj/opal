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
import org.opalj.collection.immutable.IdentityPair
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC

/**
 * This analysis determines where each field is accessed.
 *
 * ==Usage==
 * Use the [[FieldAccessInformationKey]] to query a project about the field access information.
 * {{{
 * val accessInformation = project.get(FieldAccessInformationKey)
 * }}}
 *
 * @note The analysis does not take reflective field accesses into account.
 * @note The analysis is parallelized and should not be run with other analyses in
 *      parallel.
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
            val (_ /*source*/ , _ /*classFile*/ , method) = e

            val readAccesses = AnyRefMap.empty[Field, UShortSet]
            val writeAccesses = AnyRefMap.empty[Field, UShortSet]
            val unresolved = UShortSet.empty
            method.body.get.foreach { (pc, instruction) ⇒
                instruction.opcode match {

                    case GETFIELD.opcode | GETSTATIC.opcode ⇒
                        val fieldReadAccess = instruction.asInstanceOf[FieldReadAccess]
                        classHierarchy.resolveFieldReference(fieldReadAccess, project) match {
                            case Some(field) ⇒
                                val key = field
                                readAccesses.update(
                                    key,
                                    pc +≈: readAccesses.getOrElse(key, UShortSet.empty)
                                )
                            case None ⇒
                                pc +≈: unresolved
                        }

                    case PUTFIELD.opcode | PUTSTATIC.opcode ⇒
                        val fieldWriteAccess = instruction.asInstanceOf[FieldWriteAccess]
                        classHierarchy.resolveFieldReference(fieldWriteAccess, project) match {
                            case Some(field) ⇒
                                val key = field
                                writeAccesses.update(
                                    key,
                                    pc +≈: writeAccesses.getOrElse(key, UShortSet.empty)
                                )
                            case None ⇒
                                pc +≈: unresolved
                        }

                    case _ ⇒ /*nothing to do*/
                }
            }

            // merge with the global store
            readAccesses.foreach { e ⇒
                val (key @ field, pcs) = e
                field.synchronized {
                    val currentAccesses = allReadAccesses.get(key)
                    if (currentAccesses == null)
                        allReadAccesses.put(key, (method, pcs) :: Nil)
                    else
                        allReadAccesses.put(key, (method, pcs) :: currentAccesses)
                }
            }
            writeAccesses.foreach { e ⇒
                val (key @ field, pcs) = e
                field.synchronized {
                    val currentAccesses = allWriteAccesses.get(key)
                    if (currentAccesses == null)
                        allWriteAccesses.put(key, (method, pcs) :: Nil)
                    else
                        allWriteAccesses.put(key, (method, pcs) :: currentAccesses)
                }
            }
            allUnresolved.add((method, unresolved))
        }

        import scala.collection.JavaConverters._
        val ra = (new AnyRefMap(allReadAccesses.size * 2) ++= allReadAccesses.asScala)
        ra.repack()
        val wa = (new AnyRefMap(allReadAccesses.size * 2) ++= allWriteAccesses.asScala)
        wa.repack()
        new FieldAccessInformation(project, ra, wa, allUnresolved.asScala.toIndexedSeq)
    }
}

class FieldAccessInformation(
        val project: SomeProject,
        val allReadAccesses: Map[Field, Seq[(Method, PCs)]],
        val allWriteAccesses: Map[Field, Seq[(Method, PCs)]],
        val unresolved: IndexedSeq[(Method, PCs)]) {

    private[this] def accesses(
        accessInformation: Map[Field, Seq[(Method, PCs)]],
        declaringClassType: ObjectType,
        fieldName: String): Seq[(Method, PCs)] = {
        for {
            (field, wa) ← accessInformation
            if project.classFile(field).thisType eq declaringClassType
            if field.name == fieldName
        } {
            return wa;
        }

        Seq.empty
    }

    def writeAccesses(
        declaringClassType: ObjectType,
        fieldName: String): Seq[(Method, PCs)] = {
        accesses(allWriteAccesses, declaringClassType, fieldName)
    }

    def readAccesses(
        declaringClassType: ObjectType,
        fieldName: String): Seq[(Method, PCs)] = {
        accesses(allReadAccesses, declaringClassType, fieldName)
    }

    def statistics: Map[String, Int] =
        Map(
            "field reads" -> allReadAccesses.values.map(_.map(_._2.size).sum).sum,
            "field writes" -> allWriteAccesses.values.map(_.map(_._2.size).sum).sum,
            "unresolved field accesses" -> unresolved.map(_._2.size).sum
        )

}

