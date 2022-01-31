/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

trait TypeConsumerAnalysis {
    implicit val typeProvider: TypeProvider

    type ContextType = typeProvider.ContextType
    type PropertyType = typeProvider.PropertyType
}
