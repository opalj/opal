/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package de

import scala.collection.Map
import scala.collection.Set
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br._
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

/**
 * Stores extracted dependencies.
 *
 * ==Thread Safety==
 * This class is thread safe, as this class does not have any mutable state.
 *
 * @author Michael Eichberg
 */
class DependencyStore(
        val dependencies: Map[VirtualSourceElement, Map[VirtualSourceElement, Set[DependencyType]]],
        val dependenciesOnArrayTypes: Map[VirtualSourceElement, Map[ArrayType, Set[DependencyType]]],
        val dependenciesOnBaseTypes: Map[VirtualSourceElement, Map[BaseType, Set[DependencyType]]]) {

}

object DependencyStore {

    def apply[Source](
        classFiles: Traversable[ClassFile],
        createDependencyExtractor: (DependencyProcessor) ⇒ DependencyExtractor)(
            implicit logContext: LogContext): DependencyStore = {

        val dc = time {
            val dc = new DependencyCollectingDependencyProcessor(Some(classFiles.size * 10))
            val de = createDependencyExtractor(dc)
            classFiles.par.foreach { de.process(_) }
            dc
        } { ns ⇒
            OPALLogger.info("progress", "collecting dependencies took "+ns.toSeconds)
        }

        time {
            dc.toStore
        } { ns ⇒
            OPALLogger.info("progress", "creating the dependencies store took "+ns.toSeconds)
        }
    }

    def apply[Source](
        classFiles: Traversable[ClassFile])(
            implicit logContext: LogContext): DependencyStore = {
        val createDependencyExtractor = (dp) ⇒ new DependencyExtractor(dp)
        apply(classFiles, createDependencyExtractor)
    }
}

