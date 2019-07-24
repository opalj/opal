/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.fpcf.Entity
import org.opalj.br.ArrayType
import org.opalj.br.DefinedMethod
import org.opalj.br.VirtualDeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Field

/**
 * XTA type propagation uses a separate set for each method and each field.
 *
 * @param project The project.
 * @author Andreas Bauer
 */
final class XTATypePropagationAnalysis private[analyses] (project: SomeProject)
    extends AbstractTypePropagationAnalysis(project) {

    override def getCorrespondingSetEntity(e: Entity): SetEntity = e match {
        case dm: DefinedMethod          ⇒ dm
        case vdm: VirtualDeclaredMethod ⇒ ExternalWorld
        case f: Field                   ⇒ f
        case ef: ExternalField          ⇒ ExternalWorld
        case at: ArrayType              ⇒ at
        case _                          ⇒ sys.error("unexpected entity")
    }
}

object XTATypePropagationAnalysisScheduler extends AbstractTypePropagationAnalysisScheduler(AttachToDefinedMethod) {
    override def initializeAnalysis(project: SomeProject): AbstractTypePropagationAnalysis =
        new XTATypePropagationAnalysis(project)
}

/**
 * MTA type propagation uses a separate set each field and a single set for all methods in a class.
 *
 * @param project The project.
 * @author Andreas Bauer
 */
final class MTATypePropagationAnalysis private[analyses] (project: SomeProject)
    extends AbstractTypePropagationAnalysis(project) {

    override def getCorrespondingSetEntity(e: Entity): SetEntity = e match {
        case dm: DefinedMethod          ⇒ dm.definedMethod.classFile
        case vdm: VirtualDeclaredMethod ⇒ ExternalWorld
        case f: Field                   ⇒ f
        case ef: ExternalField          ⇒ ExternalWorld
        case at: ArrayType              ⇒ at
        case _                          ⇒ sys.error("unexpected entity")
    }
}

object MTATypePropagationAnalysisScheduler extends AbstractTypePropagationAnalysisScheduler(AttachToClassFile) {
    override def initializeAnalysis(project: SomeProject): AbstractTypePropagationAnalysis =
        new MTATypePropagationAnalysis(project)
}

/**
 * FTA type propagation uses a separate set for each method and a single set for all fields in a class.
 *
 * @param project The project.
 * @author Andreas Bauer
 */
final class FTATypePropagationAnalysis private[analyses] (project: SomeProject)
    extends AbstractTypePropagationAnalysis(project) {

    override def getCorrespondingSetEntity(e: Entity): SetEntity = e match {
        case dm: DefinedMethod          ⇒ dm
        case vdm: VirtualDeclaredMethod ⇒ ExternalWorld
        case f: Field                   ⇒ f.classFile
        case ef: ExternalField          ⇒ ExternalWorld
        case at: ArrayType              ⇒ at
        case _                          ⇒ sys.error("unexpected entity")
    }
}

object FTATypePropagationAnalysisScheduler extends AbstractTypePropagationAnalysisScheduler(AttachToDefinedMethod) {
    override def initializeAnalysis(project: SomeProject): AbstractTypePropagationAnalysis =
        new FTATypePropagationAnalysis(project)
}

/**
 * CTA type propagation uses a single set for all methods and field in a class.
 *
 * @param project The project.
 * @author Andreas Bauer
 */
final class CTATypePropagationAnalysis private[analyses] (project: SomeProject)
    extends AbstractTypePropagationAnalysis(project) {

    override def getCorrespondingSetEntity(e: Entity): SetEntity = e match {
        case dm: DefinedMethod          ⇒ dm.definedMethod.classFile
        case vdm: VirtualDeclaredMethod ⇒ ExternalWorld
        case f: Field                   ⇒ f.classFile
        case ef: ExternalField          ⇒ ExternalWorld
        case at: ArrayType              ⇒ at
        case _                          ⇒ sys.error("unexpected entity")
    }
}

object CTATypePropagationAnalysisScheduler extends AbstractTypePropagationAnalysisScheduler(AttachToClassFile) {
    override def initializeAnalysis(project: SomeProject): AbstractTypePropagationAnalysis =
        new CTATypePropagationAnalysis(project)
}