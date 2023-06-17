/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.fieldaccess.FieldAccessInformation
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.collection.immutable.IntTrieSetBuilder
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable

/**
 * A simple analysis that identifies every read and write access to a [[org.opalj.br.Field]].
 *
 * @note Fields which are not accessed at all are not further considered.
 *
 * @author Maximilian Rüsch
 */
class FieldAccessInformationAnalysis(val project: SomeProject) extends FPCFAnalysis {

    private val declaredMethods = project.get(DeclaredMethodsKey)

    def analyzeMethod(method: Method): PropertyComputationResult = {
        implicit val logContext: LogContext = project.logContext
        import project.resolveFieldReference

        val readAccesses = mutable.AnyRefMap.empty[Field, IntTrieSetBuilder]
        val writeAccesses = mutable.AnyRefMap.empty[Field, IntTrieSetBuilder]

        // we don't want to report unresolvable field references multiple times
        val reportedFieldAccesses = ConcurrentHashMap.newKeySet[Instruction]()

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
                        // IMPROVE: In the old implementation, unresolvable field accesses were tracked. This can
                        // also be done here but is not needed as of now.
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
                        // IMPROVE: In the old implementation, unresolvable field accesses were tracked. This can
                        // also be done here but is not needed as of now.
                    }

                case _ => /*nothing to do*/
            }
        }

        val definedMethod = declaredMethods(method)
        val fieldAccesses = mutable.AnyRefMap.empty[Field, (List[(DefinedMethod, PCs)], List[(DefinedMethod, PCs)])]
        readAccesses foreach { readAccess =>
            val (field, pcs) = readAccess
            fieldAccesses.get(field) match {
                case Some(currentAccesses) =>
                    fieldAccesses.put(field, ((definedMethod, pcs.result()) :: currentAccesses._1, currentAccesses._2))
                case None =>
                    fieldAccesses.put(field, ((definedMethod, pcs.result()) :: Nil, Nil))
            }
        }
        writeAccesses foreach { writeAccess =>
            val (field, pcs) = writeAccess
            fieldAccesses.get(field) match {
                case Some(currentAccesses) =>
                    fieldAccesses.put(field, (currentAccesses._1, (definedMethod, pcs.result()) :: currentAccesses._2))
                case None =>
                    fieldAccesses.put(field, (Nil, (definedMethod, pcs.result()) :: Nil))
            }
        }

        Results(fieldAccesses map { fieldAccess =>
            val readAccesses = fieldAccess._2._1.to(Set)
            val writeAccesses = fieldAccess._2._2.to(Set)

            new PartialResult(
                fieldAccess._1,
                FieldAccessInformation.key,
                (current: EOptionP[Field, FieldAccessInformation]) => current match {
                    case InterimUBP(ub: FieldAccessInformation) =>
                        val newFAI = FieldAccessInformation(readAccesses, writeAccesses)

                        if (ub.equals(newFAI))
                            None
                        else
                            Some(InterimEUBP(fieldAccess._1, ub.included(newFAI)))

                    case _: EPK[_, _] =>
                        Some(InterimEUBP(fieldAccess._1, FieldAccessInformation(readAccesses, writeAccesses)))

                    case r => throw new IllegalStateException(s"unexpected previous result $r")
                }
            )
        })
    }
}

object EagerFieldAccessInformationAnalysis extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(FieldAccessInformation)

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(FieldAccessInformation)

    override def start(p: SomeProject, ps: PropertyStore, i: InitializationData): FPCFAnalysis = {
        val analysis = new FieldAccessInformationAnalysis(p)
        ps.scheduleEagerComputationsForEntities(p.allMethodsWithBody)(analysis.analyzeMethod)
        analysis
    }
}

