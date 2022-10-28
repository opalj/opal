/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.scheduling.{BasicFPCFEagerAnalysisScheduler, BasicFPCFLazyAnalysisScheduler, BasicFPCFTransformerScheduler, BasicFPCFTriggeredAnalysisScheduler, FPCFAnalysisScheduler, FPCFEagerAnalysisScheduler, FPCFLazyAnalysisScheduler, FPCFTransformerScheduler, FPCFTriggeredAnalysisScheduler}

import scala.reflect.ClassTag

trait JavaFPCFAnalysisScheduler extends FPCFAnalysisScheduler[SomeProject] {
    implicit val c: ClassTag[SomeProject] = ClassTag(classOf[SomeProject])
}

trait JavaFPCFEagerAnalysisScheduler
    extends FPCFEagerAnalysisScheduler[SomeProject]
    with JavaFPCFAnalysisScheduler {
    implicit override val c: ClassTag[SomeProject] = ClassTag(classOf[SomeProject])
}

trait JavaFPCFLazyAnalysisScheduler
    extends FPCFLazyAnalysisScheduler[SomeProject]
    with JavaFPCFAnalysisScheduler {
    implicit override val c: ClassTag[SomeProject] = ClassTag(classOf[SomeProject])
}

trait JavaFPCFTriggeredAnalysisScheduler
    extends FPCFTriggeredAnalysisScheduler[SomeProject]
    with JavaFPCFAnalysisScheduler {
    implicit override val c: ClassTag[SomeProject] = ClassTag(classOf[SomeProject])
}

trait JavaBasicFPCFLazyAnalysisScheduler
    extends BasicFPCFLazyAnalysisScheduler[SomeProject]
    with JavaFPCFAnalysisScheduler
    with JavaFPCFLazyAnalysisScheduler {
    implicit override val c: ClassTag[SomeProject] = ClassTag(classOf[SomeProject])
}

trait JavaBasicFPCFEagerAnalysisScheduler
    extends BasicFPCFEagerAnalysisScheduler[SomeProject]
    with JavaFPCFEagerAnalysisScheduler
    with JavaFPCFAnalysisScheduler {
    implicit override val c: ClassTag[SomeProject] = ClassTag(classOf[SomeProject])
}

trait JavaBasicFPCFTriggeredAnalysisScheduler
    extends BasicFPCFTriggeredAnalysisScheduler[SomeProject]
    with JavaFPCFAnalysisScheduler {
    implicit override val c: ClassTag[SomeProject] = ClassTag(classOf[SomeProject])
}

trait JavaFPCFTransformerScheduler extends FPCFTransformerScheduler[SomeProject]

trait JavaBasicFPCFTransformerScheduler extends BasicFPCFTransformerScheduler[SomeProject] with JavaFPCFTransformerScheduler