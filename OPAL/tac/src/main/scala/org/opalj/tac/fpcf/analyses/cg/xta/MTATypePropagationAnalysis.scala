/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package xta

import org.opalj.fpcf.Entity
import org.opalj.br.analyses.SomeProject
import org.opalj.br.ArrayType
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.VirtualDeclaredMethod

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