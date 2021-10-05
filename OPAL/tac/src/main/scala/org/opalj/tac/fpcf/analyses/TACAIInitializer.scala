/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses

import org.opalj.fpcf.PropertyStore
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.ai.domain.RecordDefUse
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey

/**
 * Transforms an aiResult to the 3-address code.
 *
 * @author Michael Eichberg
 */
trait TACAIInitializer extends FPCFAnalysisScheduler {

    override type InitializationData = Null

    override def init(p: SomeProject, ps: PropertyStore): Null = {
        // To compute the TAC, we (at least) need def-use information; hence, we state
        // this as a requirement.
        p.updateProjectInformationKeyInitializationData(AIDomainFactoryKey) {
            case None               => Set(classOf[RecordDefUse])
            case Some(requirements) => requirements + classOf[RecordDefUse]
        }
        null
    }

}
