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
package cg

import scala.language.postfixOps
import java.net.URL
import org.opalj.fp.Result
import org.opalj.fp.Entity
import org.opalj.fp.PropertyStore
import org.opalj.fp.PropertyKey
import org.opalj.fp.Property
import org.opalj.fp.PropertyComputationResult
import org.opalj.fp.Result
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.fp.ImmediateResult

sealed trait Instantiability extends Property {
    final def key = Instantiability.Key // All instances have to share the SAME key!
}

object Instantiability {
    final val Key = PropertyKey.create("Instantiability", Instantiable)
}

case object NotInstantiable extends Instantiability { final val isRefineable = false }

case object Instantiable extends Instantiability { final val isRefineable = false }

object InstantiabilityAnalysis {

    /**
     * Identifies those private static non-final fields that are initialized exactly once.
     */
    def determineInstantiability(
        classFile: ClassFile)(
            implicit project: SomeProject,
            projectStore: PropertyStore): PropertyComputationResult = {

        import project.classHierarchy.isSubtypeOf

        /*rule 1*/ if (classFile.constructors.exists { i ⇒ i.isPublic })
            return ImmediateResult(classFile, Instantiable);

        val isFinal = classFile.isFinal

        /*rule 2*/ if (!isFinal && classFile.constructors.exists { i ⇒ i.isProtected })
            return ImmediateResult(classFile, Instantiable);

        /*rule 3*/ if (isSubtypeOf(classFile.thisType, ObjectType.Serializable).isYesOrUnknown && classFile.constructors.exists { i ⇒ i.descriptor.parametersCount == 0 })
            return ImmediateResult(classFile, Instantiable);

        //if (classFile.constructors.forall(i ⇒ i.isPrivate))

        // Fallback:
        Result(classFile, Instantiable)
    }

    def analyze(implicit project: Project[URL]): Unit = {
        implicit val projectStore = project.get(SourceElementsPropertyStoreKey)
        val filter: PartialFunction[Entity, ClassFile] = {
            case cf: ClassFile ⇒ cf
        }
        projectStore <||< (filter, determineInstantiability)
    }
}

