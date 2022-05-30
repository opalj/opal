/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.br.fpcf.properties.Context

trait ContextualAnalysis {
    type ContextType <: Context
}
