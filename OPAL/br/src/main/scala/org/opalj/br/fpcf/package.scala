/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.br.analyses.SomeProject

package object fpcf {

    type FPCFAnalysisScheduler = org.opalj.fpcf.FPCFAnalysisScheduler[SomeProject]
    type FPCFEagerAnalysisScheduler = org.opalj.fpcf.FPCFEagerAnalysisScheduler[SomeProject]
    type FPCFLazyAnalysisScheduler = org.opalj.fpcf.FPCFLazyAnalysisScheduler[SomeProject]
    type FPCFTransformerScheduler = org.opalj.fpcf.FPCFTransformerScheduler[SomeProject]
    type FPCFTriggeredAnalysisScheduler = org.opalj.fpcf.FPCFTriggeredAnalysisScheduler[SomeProject]
    type BasicFPCFEagerAnalysisScheduler = org.opalj.fpcf.BasicFPCFEagerAnalysisScheduler[SomeProject]
    type BasicFPCFLazyAnalysisScheduler = org.opalj.fpcf.BasicFPCFLazyAnalysisScheduler[SomeProject]
    type BasicFPCFTransformerScheduler = org.opalj.fpcf.BasicFPCFTransformerScheduler[SomeProject]
    type BasicFPCFTriggeredAnalysisScheduler = org.opalj.fpcf.BasicFPCFTriggeredAnalysisScheduler[SomeProject]
}
