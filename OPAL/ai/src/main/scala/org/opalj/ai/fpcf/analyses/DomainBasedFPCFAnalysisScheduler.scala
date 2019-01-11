/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package fpcf
package analyses

import org.opalj.fpcf.FPCFAnalysisScheduler
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.ai.omain.PropertyStoreBased

/**
 * Scheduler that can be used by analyses which perform abstract interpretations and where
 * the used properties are defined by the domain.
 *
 * @author Michael Eichberg
 */
trait DomainBasedFPCFAnalysisScheduler extends FPCFAnalysisScheduler {

    override def uses(p: SomeProject, ps: PropertyStore): Set[PropertyBounds] = {
        if (p.allMethodsWithBody.isEmpty)
            return Set.empty;

        // To get the domain's requirements, we simply instatiate it and query it...
        val domain = p.get(AIDomainFactoryKey).domainFactory(p, p.allMethodsWithBody.head)
        domain match {
            case d: PropertyStoreBased ⇒ d.usesPropertyBounds
            case _                     ⇒ Set.empty
        }
    }

    final override def uses: Set[PropertyBounds] = Set.empty
}

