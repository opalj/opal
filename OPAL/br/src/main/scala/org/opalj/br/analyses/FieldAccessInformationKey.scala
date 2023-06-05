/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.properties.{FieldAccessInformation => IndividualFieldAccessInformation}
import org.opalj.br.fpcf.analyses.EagerFieldAccessInformationAnalysis
import org.opalj.fpcf.FinalEP

import scala.collection.mutable

/**
 * The ''key'' object to get global field access information.
 *
 * @example To get the index use the [[Project]]'s `get` method and pass in `this` object.
 *
 * @author Michael Eichberg
 */
object FieldAccessInformationKey extends ProjectInformationKey[FieldAccessInformation, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys =
      EagerFieldAccessInformationAnalysis.requiredProjectInformation

    /**
     * Computes the field access information.
     */
    override def compute(project: SomeProject): FieldAccessInformation = {
        val propertyStore = project.get(FPCFAnalysesManagerKey).runAll(EagerFieldAccessInformationAnalysis)._1

        propertyStore.waitOnPhaseCompletion()

        val allReadAccesses = mutable.AnyRefMap.empty[Field, Seq[(Method, PCs)]]
        val allWriteAccesses = mutable.AnyRefMap.empty[Field, Seq[(Method, PCs)]]
        propertyStore.entities(IndividualFieldAccessInformation.key) foreach {
          case FinalEP(e, p) =>
            allReadAccesses.put(e.asInstanceOf[Field], p.readAccesses.toSeq)
            allWriteAccesses.put(e.asInstanceOf[Field], p.writeAccesses.toSeq)
          case _ =>
        }

       new FieldAccessInformation(project, allReadAccesses, allWriteAccesses, unresolved = Vector())
    }
}

