/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org
package opalj
package tac
package fpcf
package analyses
package string

import org.opalj.br.DefinedMethod
import org.opalj.br.Method
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Property
import org.opalj.tac.fpcf.analyses.string.preprocessing.Path
import org.opalj.tac.fpcf.properties.TACAI

/**
 * This class is to be used to store state information that are required at a later point in
 * time during the analysis, e.g., due to the fact that another analysis had to be triggered to
 * have all required information ready for a final result.
 */
case class ComputationState(dm: DefinedMethod, entity: SContext) {

    /**
     * The Three-Address Code of the entity's method
     */
    var tac: TAC = _

    /**
     * The computed lean path that corresponds to the given entity
     */
    var computedLeanPaths: Seq[Path] = _

    var tacDependee: Option[EOptionP[Method, TACAI]] = _

    /**
     * If not empty, this routine can only produce an intermediate result
     */
    var dependees: List[EOptionP[DUSiteEntity, Property]] = List()
}

case class DUSiteState(pc: Int, dm: DefinedMethod, tac: TAC, entity: SEntity)

private[string] case class InterpretationState(pc: Int, dm: DefinedMethod, var tacDependee: EOptionP[Method, TACAI]) {

    def tac: TAC = {
        if (tacDependee.hasUBP && tacDependee.ub.tac.isDefined)
            tacDependee.ub.tac.get
        else
            throw new IllegalStateException("Cannot get a tac from a TACAI with no or empty upper bound!")
    }
}
