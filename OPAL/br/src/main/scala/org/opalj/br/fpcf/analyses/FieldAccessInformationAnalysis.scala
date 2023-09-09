/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.DefinedFieldsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.SimpleContext
import org.opalj.br.fpcf.properties.fieldaccess.DirectFieldAccesses
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldWriteAccessInformation
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

import java.util.concurrent.ConcurrentHashMap

/**
 * A simple analysis that identifies every direct read and write access to a [[org.opalj.br.Field]].
 *
 * @note Fields which are not accessed at all are not further considered.
 *
 * @author Maximilian Rüsch
 */
class FieldAccessInformationAnalysis(val project: SomeProject) extends FPCFAnalysis {

    private val declaredMethods = project.get(DeclaredMethodsKey)
    private val definedFields = project.get(DefinedFieldsKey)

    def analyzeMethod(method: Method): PropertyComputationResult = {
        implicit val logContext: LogContext = project.logContext
        import project.resolveFieldReference

        val context = SimpleContext(declaredMethods(method));
        val fieldAccesses = new DirectFieldAccesses()

        // we don't want to report unresolvable field references multiple times
        val reportedFieldAccesses = ConcurrentHashMap.newKeySet[Instruction]()

        method.body.get iterate { (pc, instruction) =>
            instruction.opcode match {
                case GETFIELD.opcode | GETSTATIC.opcode =>
                    val fieldReadAccess = instruction.asInstanceOf[FieldReadAccess]
                    resolveFieldReference(fieldReadAccess) match {
                        case Some(field) =>
                            fieldAccesses.addFieldRead(context, pc, definedFields(field))
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
                            fieldAccesses.addFieldWrite(context, pc, definedFields(field))
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

        Results(fieldAccesses.partialResults(context))
    }
}

object EagerFieldAccessInformationAnalysis extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredMethodsKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        FieldReadAccessInformation,
        FieldWriteAccessInformation,
        MethodFieldReadAccessInformation,
        MethodFieldWriteAccessInformation
    )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        FieldReadAccessInformation,
        FieldWriteAccessInformation,
        MethodFieldReadAccessInformation,
        MethodFieldWriteAccessInformation
    )

    override def start(p: SomeProject, ps: PropertyStore, i: InitializationData): FPCFAnalysis = {
        val analysis = new FieldAccessInformationAnalysis(p)
        ps.scheduleEagerComputationsForEntities(p.allMethodsWithBody)(analysis.analyzeMethod)
        analysis
    }
}

