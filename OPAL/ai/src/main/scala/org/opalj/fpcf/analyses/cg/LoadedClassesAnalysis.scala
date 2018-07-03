/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package analyses
package cg

import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.tac.Assignment
import org.opalj.tac.GetField
import org.opalj.tac.PutStatic
import org.opalj.tac.SimpleTACAIKey

class LoadedClassesAnalysis(
        private[this] val project:           SomeProject,
        private[this] val callGraphAnalysis: CallGraphAnalysis
) {
    private val tacaiProvider = project.get(SimpleTACAIKey)
    private val declaredMethods = project.get(DeclaredMethodsKey)

    def newReachableMethod(dm: DeclaredMethod): PropertyComputationResult = {
        assert(dm.hasSingleDefinedMethod)
        val method = dm.definedMethod
        assert(dm.declaringClassType eq method.classFile.thisType)

        var newReachableMethods = List.empty[DeclaredMethod]

        for (stmt ← tacaiProvider(method)) {
            stmt match {
                case PutStatic(_, dc, _, _, _) ⇒
                    handleType(dc).foreach(newReachableMethods +:= _)
                case Assignment(_, _, GetField(_, dc, _, _, _)) ⇒
                    handleType(dc).foreach(newReachableMethods +:= _)
            }
        }

        IncrementalResult(
            NoResult,
            newReachableMethods.map(m ⇒ (callGraphAnalysis.processMethod _, m))
        )
    }

    def handleType(declaringClassType: ObjectType): Option[DefinedMethod] = {
        project.classFile(declaringClassType).flatMap { cf ⇒
            cf.staticInitializer.flatMap { clInit ⇒
                val clInitDM = declaredMethods(clInit)
                Some(clInitDM).filter(callGraphAnalysis.registerMethodToProcess)
            }
        }
    }
}
