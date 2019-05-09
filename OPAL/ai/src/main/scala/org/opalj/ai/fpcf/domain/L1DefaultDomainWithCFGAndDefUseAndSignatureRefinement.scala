/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package domain

import org.opalj.fpcf.EOptionPSet
import org.opalj.fpcf.FinalProperties
import org.opalj.fpcf.SinglePropertiesBoundType
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property
import org.opalj.br.analyses.Project
import org.opalj.br.Method
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse

class L1DefaultDomainWithCFGAndDefUseAndSignatureRefinement[Source](
        project:       Project[Source],
        method:        Method,
        val dependees: EOptionPSet[Entity, Property] = EOptionPSet.empty
) extends DefaultDomainWithCFGAndDefUse[Source](project, method)
    with RefinedTypeLevelFieldAccessInstructions
    with RefinedTypeLevelInvokeInstructions {

    // We specify that we are able to only handle final properties to avoid that users
    // of this domain have to worry about intermediate updates.
    final val UsedPropertiesBound: SinglePropertiesBoundType = FinalProperties

}
