/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.scheduling.{BasicFPCFEagerAnalysisScheduler, BasicFPCFLazyAnalysisScheduler, FPCFAnalysisScheduler, FPCFEagerAnalysisScheduler, FPCFLazyAnalysisScheduler}

import scala.reflect.{ClassTag, classTag}

trait JavaFPCFAnalysisScheduler extends FPCFAnalysisScheduler[SomeProject] {
    implicit val c: ClassTag[SomeProject] = classTag[SomeProject]
}

trait JavaFPCFEagerAnalysisScheduler extends FPCFEagerAnalysisScheduler[SomeProject] {
    implicit val c: ClassTag[SomeProject] = classTag[SomeProject]
}

trait JavaFPCFLazyAnalysisScheduler extends FPCFLazyAnalysisScheduler[SomeProject] {
    implicit val c: ClassTag[SomeProject] = classTag[SomeProject]
}

trait JavaBasicFPCFLazyAnalysisScheduler extends BasicFPCFLazyAnalysisScheduler[SomeProject] with JavaFPCFLazyAnalysisScheduler {
    override implicit val c: ClassTag[SomeProject] = classTag[SomeProject]
}

trait JavaBasicFPCFEagerAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler[SomeProject] with JavaFPCFEagerAnalysisScheduler {
    override implicit val c: ClassTag[SomeProject] = classTag[SomeProject]
}