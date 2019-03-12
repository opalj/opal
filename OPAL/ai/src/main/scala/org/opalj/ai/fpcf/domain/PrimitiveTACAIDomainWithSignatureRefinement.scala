/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package domain

import org.opalj.fpcf.Property
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionPSet
import org.opalj.fpcf.SinglePropertiesBoundType
import org.opalj.fpcf.FinalProperties
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.domain.l0.TypeLevelDomain
import org.opalj.ai.domain.l0.DefaultTypeLevelHandlingOfMethodResults
import org.opalj.ai.domain.IgnoreSynchronization
import org.opalj.ai.domain.TheMethod
import org.opalj.ai.domain.ThrowAllPotentialExceptionsConfiguration
import org.opalj.ai.domain.RecordDefUse

/**
 * This is a primitive domain that can be used to transform Java bytecode to the
 * three address representation offered by OPAL, which is build upon the result
 * of a lightweight abstract interpretation.
 */
class PrimitiveTACAIDomainWithSignatureRefinement(
        val project:   SomeProject,
        val method:    Method,
        val dependees: EOptionPSet[Entity, Property] = EOptionPSet.empty
) extends TypeLevelDomain
    with ThrowAllPotentialExceptionsConfiguration
    with IgnoreSynchronization
    with DefaultTypeLevelHandlingOfMethodResults
    with TheMethod
    with RecordDefUse
    with RefinedTypeLevelFieldAccessInstructions
    with RefinedTypeLevelInvokeInstructions {

    val UsedPropertiesBound: SinglePropertiesBoundType = FinalProperties

}

