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

import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.ClassFile
import scala.collection.Set
import scala.collection.mutable.HashSet

/**
 * A very basic analysis which identifies those classes that can never be instantiated (e.g.,
 * `java.lang.Math`).
 *
 * For details: see org.opalj.fpcf.analysis.SimpleInstantiableClassesAnalysis
 *
 * ==Usage==
 * Use the [[InstantiableClassesIndexKey]] to query a project about the instantiable classes.
 * {{{
 * val instantiableClasses = project.get(InstantiableClassesIndexKey)
 * }}}
 *
 * @note The analysis does not take reflective instantiations into account!
 *
 * @author Michael Eichberg
 */
class InstantiableClassesIndex private (
        val project:         SomeProject,
        val notInstantiable: Set[ObjectType]
) {

    def isInstantiable(objectType: ObjectType): Boolean = !notInstantiable.contains(objectType)
}

/**
 * Stores the information about those classes that are not instantiable (which is
 * usually only a small fraction of all classes and hence, more
 * efficient to store/access).
 *
 * @author MichaelReif
 */
object InstantiableClassesIndex {

    def apply(project: SomeProject): InstantiableClassesIndex = {
        val fpcfManager = project.get(FPCFAnalysisManagerKey)
        if (!fpcfManager.isDerived(Instantiability))
            fpcfManager.run(SimpleInstantiabilityAnalysis, true)

        val propertyStore = project.get(SourceElementsPropertyStoreKey)
        val notInstantiableClasses = propertyStore.collect[ObjectType] {
            case (cf: ClassFile, NotInstantiable) ⇒ cf.thisType
        }

        new InstantiableClassesIndex(project, HashSet.empty ++ notInstantiableClasses.toSet)
    }
}
