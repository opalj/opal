/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.br.analyses.ProjectBasedAnalysis
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.tac.cg.TypeIteratorKey

trait TypeConsumerAnalysis extends ProjectBasedAnalysis {
    implicit val typeIterator: TypeIterator = project.get(TypeIteratorKey)
    implicit val contextProvider: ContextProvider = typeIterator

    type ContextType = typeIterator.ContextType
    type PropertyType = typeIterator.PropertyType
}
