/**
 * BSD 2-Clause License:
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
package fpa

import java.net.URL
import org.opalj.fp.Entity
import org.opalj.fp.PropertyStore
import org.opalj.fp.PropertyKey
import org.opalj.fp.Property
import org.opalj.fp.PropertyComputationResult
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.analyses.SomeProject
import org.opalj.fp.ImmediateResult
import org.opalj.br.Method

sealed trait EntryPoint extends Property {
    final def key = Instantiability.Key // All instances have to share the SAME key!
}

object EntryPoint {
    final val Key = PropertyKey.create("EntryPoint", DirectlyCallable)
}

case object DirectlyCallable extends EntryPoint { final val isRefineable = false }

case object OnlyIndirectlyCallable extends EntryPoint { final val isRefineable = false }

object EntryPointsAnalysis {

    /**
     * Identifies those private static non-final fields that are initialized exactly once.
     */
    def determineEntryPoints(
        method: Method)(
            implicit project: SomeProject,
            projectStore: PropertyStore): PropertyComputationResult = {

        //import project.classHierarchy.isSubtypeOf

        val declClass = project.classFile(method)
        val declClassIsPublic = declClass.isPublic
        val declClassIsFinal = declClass.isFinal

        /*rule 1.1*/ if (method.isStaticInitializer && declClassIsPublic)
            return ImmediateResult(method, DirectlyCallable);

        val isStatic = method.isStatic
        val isPublic = method.isPublic

        /*rule 2*/ if (isStatic && isPublic)
            return ImmediateResult(method, DirectlyCallable);

        val isConstructor = method.isConstructor

        /*rule 4*/ if (isConstructor && isPublic)
            return ImmediateResult(method, DirectlyCallable);

        val isProtected = method.isProtected

        /*rule 3*/ if (isStatic && isProtected && !declClassIsFinal)
            return ImmediateResult(method, DirectlyCallable);

        ///*rule 5*/ if (isConstructor && isProtected && !declClassIsFinal)

        // Fallback
        return ImmediateResult(method, DirectlyCallable);
    }

    def analyze(implicit project: SomeProject): Unit = {
        implicit val projectStore = project.get(SourceElementsPropertyStoreKey)
        val filter: PartialFunction[Entity, Method] = {
            case m: Method if !m.isAbstract ⇒ m
        }
        projectStore <||< (filter, determineEntryPoints)
    }
}

