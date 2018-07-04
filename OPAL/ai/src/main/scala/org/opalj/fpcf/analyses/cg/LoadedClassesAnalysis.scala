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
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.properties.CallersProperty
import org.opalj.fpcf.properties.LoadedClasses
import org.opalj.fpcf.properties.NoCallers
import org.opalj.tac.Assignment
import org.opalj.tac.GetField
import org.opalj.tac.PutStatic
import org.opalj.tac.SimpleTACAIKey

class LoadedClassesState(
        private[this] var _loadedClasses: UIDSet[ObjectType],
        private[this] var _dependees:     Set[EOptionP[DeclaredMethod, CallersProperty]]
) {
    def loadedClasses: UIDSet[ObjectType] = _loadedClasses

    def addLoadedType(objectType: ObjectType): Unit = _loadedClasses += objectType

    def dependees: Traversable[EOptionP[DeclaredMethod, CallersProperty]] = _dependees

    def addDependee(dependee: EOptionP[DeclaredMethod, CallersProperty]): Unit = {
        _dependees += dependee
    }
    def removeDependee(epk: EPK[DeclaredMethod, CallersProperty]): Unit = {
        assert(_dependees.count(dependee ⇒ (dependee.e eq epk.e) && dependee.pk == epk.pk) <= 1)
        _dependees = _dependees.filter(dependee ⇒ (dependee.e ne epk.e) || dependee.pk != epk.pk)
    }
    def updateDependee(newEPS: EPS[DeclaredMethod, CallersProperty]): Unit = {
        removeDependee(newEPS.toEPK)
        addDependee(newEPS)
    }
}

class LoadedClassesAnalysis(
        private[this] val project:           SomeProject,
        private[this] val callGraphAnalysis: CallGraphAnalysis
) {
    private val tacaiProvider = project.get(SimpleTACAIKey)
    private val declaredMethods = project.get(DeclaredMethodsKey)
    private val propertyStore = project.get(PropertyStoreKey)

    def doAnalyze(): PropertyComputationResult = {
        var reachableMethods = List.empty[DeclaredMethod]
        val state = new LoadedClassesState(UIDSet.empty, Set.empty)

        // todo: here we should depend on all updates
        for (m ← project.allMethods) {
            val dm = declaredMethods(m)
            reachableMethods ++= handleCaller(state, propertyStore(dm, CallersProperty.key))
        }

        returnResult(reachableMethods, state)
    }

    def handleCaller(
        state: LoadedClassesState, eOptP: EOptionP[DeclaredMethod, CallersProperty]
    ): List[DeclaredMethod] = {
        var reachableMethods = List.empty[DeclaredMethod]
        eOptP match {
            case FinalEP(_, NoCallers) ⇒
                // the method was not called at all
                state.removeDependee(eOptP.toEPK)
            case _: EPK[DeclaredMethod, CallersProperty] ⇒
                state.addDependee(eOptP)
            case eps @ EPS(_, _, NoCallers) ⇒
                state.updateDependee(eps)
            case eps @ EPS(dm: DeclaredMethod, _, _) ⇒
                state.removeDependee(eps.toEPK)

                // the method has callers. we have to analyze it
                val newReachableMethods = handleNewReachableMethod(dm, state)
                reachableMethods ++= newReachableMethods
        }
        reachableMethods
    }

    def handleNewReachableMethod(
        dm: DeclaredMethod, state: LoadedClassesState
    ): List[DeclaredMethod] = {
        assert(dm.hasSingleDefinedMethod)
        val method = dm.definedMethod
        val methodDCT = method.classFile.thisType
        assert(dm.declaringClassType eq methodDCT)

        var newReachableMethods = List.empty[DeclaredMethod]

        // whenever a method is called the first time, its declaring class gets loaded
        if (!state.loadedClasses.contains(method.classFile.thisType)) {
            handleType(method.classFile.thisType).foreach(newReachableMethods +:= _)
            state.addLoadedType(methodDCT)
        }

        for (stmt ← tacaiProvider(method).stmts) {
            stmt match {
                case PutStatic(_, dct, _, _, _) ⇒
                    handleType(dct).foreach(newReachableMethods +:= _)
                    state.addLoadedType(dct)
                case Assignment(_, _, GetField(_, dct, _, _, _)) ⇒
                    handleType(dct).foreach(newReachableMethods +:= _)
                    state.addLoadedType(dct)
                case _ ⇒
            }
        }

        newReachableMethods
    }

    def handleType(declaringClassType: ObjectType): Option[DefinedMethod] = {
        project.classFile(declaringClassType).flatMap { cf ⇒
            cf.staticInitializer.flatMap { clInit ⇒
                val clInitDM = declaredMethods(clInit)
                Some(clInitDM).filter(callGraphAnalysis.registerMethodToProcess)
            }
        }
    }

    def returnResult(
        reachableMethods: Traversable[DeclaredMethod], state: LoadedClassesState
    ): PropertyComputationResult = {
        val loadedClassesUB = new LoadedClasses(state.loadedClasses)

        val loadedClassesResult = if (state.dependees.isEmpty) {
            Result(project, new LoadedClasses(state.loadedClasses))
        } else {
            IntermediateResult(
                project, null /*TODO LB */ , loadedClassesUB, state.dependees, continuation(state)
            )
        }

        IncrementalResult(
            loadedClassesResult,
            reachableMethods.map(m ⇒ (callGraphAnalysis.processMethod(isEntryPoint = true) _, m))
        )
    }

    def continuation(state: LoadedClassesState)(eps: SomeEPS): PropertyComputationResult = {
        val reachableMethods =
            handleCaller(state, eps.asInstanceOf[EPS[DeclaredMethod, CallersProperty]])

        returnResult(reachableMethods, state)
    }
}
