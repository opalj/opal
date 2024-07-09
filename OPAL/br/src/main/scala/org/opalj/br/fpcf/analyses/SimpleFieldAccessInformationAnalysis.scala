/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.properties.fieldaccess.DirectFieldAccesses
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldWriteAccessInformation
import org.opalj.br.instructions.FieldReadAccess
import org.opalj.br.instructions.FieldWriteAccess
import org.opalj.br.instructions.GETFIELD
import org.opalj.br.instructions.GETSTATIC
import org.opalj.br.instructions.PUTFIELD
import org.opalj.br.instructions.PUTSTATIC
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results

/**
 * A simple analysis that identifies every direct read and write access to a [[org.opalj.br.Field]] without using
 * receiver or value information. If you need receiver or value information execute the similar analysis from TAC.
 *
 * @note Fields which are not accessed at all are not further considered.
 * @note This analysis should never be executed together with its corresponding analysis from TAC to prevent overrides
 *       of receiver / value information.
 *
 * @author Maximilian Rüsch
 */
class SimpleFieldAccessInformationAnalysis(val project: SomeProject) extends FPCFAnalysis {

    private val declaredMethods = project.get(DeclaredMethodsKey)
    private val declaredFields = project.get(DeclaredFieldsKey)
    private val contextProvider = project.get(ContextProviderKey)

    def analyzeMethod(method: Method): PropertyComputationResult = {
        val context = contextProvider.newContext(declaredMethods(method))
        implicit val fieldAccesses: DirectFieldAccesses = new DirectFieldAccesses()

        method.body.get iterate { (pc, instruction) =>
            instruction.opcode match {
                case GETFIELD.opcode | GETSTATIC.opcode =>
                    fieldAccesses.addFieldRead(
                        context,
                        pc,
                        declaredFields(instruction.asInstanceOf[FieldReadAccess]),
                        None
                    )

                case PUTFIELD.opcode | PUTSTATIC.opcode =>
                    fieldAccesses.addFieldWrite(
                        context,
                        pc,
                        declaredFields(instruction.asInstanceOf[FieldWriteAccess]),
                        None,
                        None
                    )

                case _ => /*nothing to do*/
            }
        }

        Results(fieldAccesses.partialResults(context))
    }
}

object EagerSimpleFieldAccessInformationAnalysis extends BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(
        DeclaredMethodsKey,
        DeclaredFieldsKey,
        ContextProviderKey
    )

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
        val analysis = new SimpleFieldAccessInformationAnalysis(p)
        ps.scheduleEagerComputationsForEntities(p.allMethodsWithBody)(analysis.analyzeMethod)
        analysis
    }
}
