/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package fpcf
package analysis
package cg
package cha

import org.opalj.br.Method
import org.opalj.fpcf.Property
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.analyses.CallBySignatureResolutionKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
/**
 * EXPERIMENTAL CAN BE DELETED LATER ON THIS CALLGRAPH USES THE NEW ENTRY POINTS AND BUILDS THE CG WITHOUT SIGNATURE RESOLUTION.
 */
object ExpLibraryCHACallGraphKey extends ProjectInformationKey[ComputedCallGraph] {

    /**
     * The CHACallGraph has no special prerequisites.W
     *
     * @return `Nil`.
     */
    override protected def requirements = Seq(CallBySignatureResolutionKey)

    /**
     * Computes the `CallGraph` for the given project.
     */
    override protected def compute(project: SomeProject): ComputedCallGraph = {
        val propertyStore = project.get(SourceElementsPropertyStoreKey)
        val entryPointSet: scala.collection.mutable.Set[Method] = propertyStore.entities { (p: Property) ⇒
            p == IsEntryPoint
        }.collect { case entity: Method ⇒ entity }

        println("startConstruction")
        CallGraphFactory.create(
            project, () ⇒ entryPointSet,
            new CHACallGraphAlgorithmConfiguration(project)
        )
    }
}