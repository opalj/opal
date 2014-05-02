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
package de.tud.cs.st
package bat
package resolved
package dependency

import analyses.SomeProject
import analyses.ProjectInformationKey

import scala.collection.Map
import scala.collection.Set

/**
 * Stores extracted dependencies.
 *
 * ==Thread Safety==
 * This class is thread safe, as this class does not have any mutable state.
 *
 * @author Michael Eichberg
 */
class DependencyStore(
        val dependencies: Map[VirtualSourceElement, Map[VirtualSourceElement, Long]],
        val dependenciesOnArrayTypes: Map[VirtualSourceElement, Map[ArrayType, Set[DependencyType]]],
        val dependenciesOnBaseTypes: Map[VirtualSourceElement, Map[BaseType, Set[DependencyType]]]) {

}

object DependencyStore {

    def initialize[Source](
        classFiles: Traversable[ClassFile],
        createDependencyExtractor: (DependencyProcessor) ⇒ DependencyExtractor): DependencyStore = {

        import util.debug.PerformanceEvaluation.{ time, ns2sec }
        val dc = time {
            val dc = new DependencyCollectingDependencyProcessor(Some(classFiles.size * 10))
            val de = createDependencyExtractor(dc)
            for (classFile ← classFiles.par) {
                de.process(classFile)
            }
            dc
        } { t ⇒ println("[info] Collecting dependencies:"+ns2sec(t)) }

        time {
            dc.toStore
        } { t ⇒ println("[info] Creating dependencies store: "+ns2sec(t)) }
    }

    def initialize[Source](
        classFiles: Traversable[ClassFile]): DependencyStore = {
        val createDependencyExtractor =
            (dp: DependencyProcessor) ⇒ new DependencyExtractor(dp)
        initialize(classFiles, createDependencyExtractor)
    }
}

object DependencyStoreKey extends ProjectInformationKey[DependencyStore] {

    override protected def requirements: Seq[ProjectInformationKey[_ <: AnyRef]] = Nil

    override protected def compute(project: SomeProject): DependencyStore = {
        DependencyStore.initialize(project.classFiles)
    }
}

object DependencyStoreWithoutSelfDependenciesKey extends ProjectInformationKey[DependencyStore] {

    override protected def requirements: Seq[ProjectInformationKey[_ <: AnyRef]] = Nil

    override protected def compute(project: SomeProject): DependencyStore = {
        DependencyStore.initialize(
            project.classFiles,
            (dp: DependencyProcessor) ⇒
                new DependencyExtractor(
                    new DependencyProcessorDecorator(dp) with FilterSelfDependencies
                )
        )
    }
}


