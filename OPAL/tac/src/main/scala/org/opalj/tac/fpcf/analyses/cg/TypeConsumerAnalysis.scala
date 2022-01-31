/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.br.analyses.ProjectBasedAnalysis
import org.opalj.tac.cg.TypeProviderKey

trait TypeConsumerAnalysis extends ProjectBasedAnalysis {
    implicit val typeProvider: TypeProvider = project.get(TypeProviderKey)

    type ContextType = typeProvider.ContextType
    type PropertyType = typeProvider.PropertyType
}
