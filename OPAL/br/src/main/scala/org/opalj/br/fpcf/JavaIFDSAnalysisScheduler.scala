/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.ifds.{AbstractIFDSFact, IFDSAnalysisScheduler, Statement}

import scala.reflect.ClassTag

trait JavaIFDSAnalysisScheduler[IFDSFact <: AbstractIFDSFact, C <: AnyRef, S <: Statement[_ <: C, _]]
    extends IFDSAnalysisScheduler[SomeProject, IFDSFact, C, S] {
    implicit val c: ClassTag[SomeProject] = ClassTag(classOf[SomeProject])
}
