/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.mutable.AnyRefMap

import org.opalj.log.OPALLogger
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.br.instructions.Instruction
import org.opalj.collection.immutable.IntTrieSetBuilder

/**
 * This analysis determines where each field is accessed.
 *
 * ==Usage==
 * Use the [[FieldAccessInformationKey]] to query a project about the field access information.
 * {{{
 * val accessInformation = project.get(FieldAccessInformationKey)
 * }}}
 *
 * @note    The analysis does not take reflective field accesses into account.
 * @note    The analysis is internally parallelized and should not be run with other analyses in
 *          parallel.
 * @note    Fields which are not accessed at all are not further considered.
 *
 * @author Michael Eichberg
 */
object FieldAccessInformationAnalysis {

    def doAnalyze(project: SomeProject, isInterrupted: () => Boolean): FieldAccessInformation = {
        import project.resolveFieldReference
        import project.logContext

        val allReadAccesses = new ConcurrentHashMap[Field, List[(Method, PCs)]]()
        val allWriteAccesses = new ConcurrentHashMap[Field, List[(Method, PCs)]]()
        val allUnresolved = new ConcurrentLinkedQueue[(Method, PCs)]()

        // we don't want to report unresolvable field references multiple times
        val reportedFieldAccesses = ConcurrentHashMap.newKeySet[Instruction]()

        project.parForeachMethodWithBody(isInterrupted) { methodInfo =>
            val method = methodInfo.method

            val readAccesses = AnyRefMap.empty[Field, IntTrieSetBuilder]
            val writeAccesses = AnyRefMap.empty[Field, IntTrieSetBuilder]
            var unresolved = IntTrieSet.empty
            method.body.get iterate { (pc, instruction) =>
                instruction.opcode match {

                    case GETFIELD.opcode | GETSTATIC.opcode =>
                        val fieldReadAccess = instruction.asInstanceOf[FieldReadAccess]
                        resolveFieldReference(fieldReadAccess) match {
                            case Some(field) =>
                                readAccesses.getOrElseUpdate(field, new IntTrieSetBuilder()) += pc
                            case None =>
                                if (reportedFieldAccesses.add(instruction)) {
                                    val message = s"cannot resolve field read access: $instruction"
                                    OPALLogger.warn("project configuration", message)
                                }
                                unresolved += pc
                        }

                    case PUTFIELD.opcode | PUTSTATIC.opcode =>
                        val fieldWriteAccess = instruction.asInstanceOf[FieldWriteAccess]
                        resolveFieldReference(fieldWriteAccess) match {
                            case Some(field) =>
                                writeAccesses.getOrElseUpdate(field, new IntTrieSetBuilder()) += pc
                            case None =>
                                if (reportedFieldAccesses.add(instruction)) {
                                    val message = s"cannot resolve field write access: $instruction"
                                    OPALLogger.warn("project configuration", message)
                                }
                                unresolved += pc
                        }

                    case _ => /*nothing to do*/
                }
            }

            // merge with the global store
            readAccesses foreach { e =>
                val (key @ field, pcs) = e
                field.synchronized {
                    val currentAccesses = allReadAccesses.get(key)
                    if (currentAccesses eq null)
                        allReadAccesses.put(key, (method, pcs.result()) :: Nil)
                    else
                        allReadAccesses.put(key, (method, pcs.result()) :: currentAccesses)
                }
            }
            writeAccesses foreach { e =>
                val (key @ field, pcs) = e
                field.synchronized {
                    val currentAccesses = allWriteAccesses.get(key)
                    if (currentAccesses eq null)
                        allWriteAccesses.put(key, (method, pcs.result()) :: Nil)
                    else
                        allWriteAccesses.put(key, (method, pcs.result()) :: currentAccesses)
                }
            }

            if (unresolved.nonEmpty) allUnresolved.add((method, unresolved))
        }

        import scala.jdk.CollectionConverters._
        val ra = new AnyRefMap(allReadAccesses.size * 2) ++= allReadAccesses.asScala
        ra.repack()
        val wa = new AnyRefMap(allReadAccesses.size * 2) ++= allWriteAccesses.asScala
        wa.repack()
        new FieldAccessInformation(project, ra, wa, allUnresolved.asScala.toVector)
    }
}
