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

/**
 * Computes the set of classes that are being loaded by the VM during the execution of the
 * `project`.
 * @author Florian Kuebler
 */
class LoadedClassesAnalysis(
        val project:                         SomeProject,
        private[this] val callGraphAnalysis: CallGraphAnalysis
) extends FPCFAnalysis {
    private val tacaiProvider = project.get(SimpleTACAIKey)
    private val declaredMethods = project.get(DeclaredMethodsKey)
    private val propertyStore = project.get(PropertyStoreKey)

    /**
     * Each time a method gets reachable in the computation of the call graph
     * (callers are added/it is an entry point [[CallersProperty]]) the declaring class gets loaded
     * (if not already done) by the VM. Furthermore, access to static fields yields the VM to load
     * a class. So for a new reachable method, we further check for such operations.
     * For newly loaded classes, the analysis triggers the computation of the call graph properties
     * ([[org.opalj.fpcf.properties.Callees]], [[org.opalj.fpcf.properties.CallersProperty]]) for
     * the static initializer.
     *
     */
    def doAnalyze(project: SomeProject): PropertyComputationResult = {
        // todo use a better data structure here
        var newReachableMethods = List.empty[DeclaredMethod]
        val state = new LoadedClassesState(UIDSet.empty, Set.empty)

        // todo: here we should depend on all updates
        for (m ← project.allMethods) {
            val dm = declaredMethods(m)
            // check if the method is reachable and handle it correspondingly
            newReachableMethods ++= handleCaller(state, propertyStore(dm, CallersProperty.key))
        }

        // if there are open dependencies left, return an intermediate result
        // force call graph computation for new reachable methods
        returnResult(newReachableMethods, state)
    }

    /**
     * If the method in `callersOfMethod` has no callers ([[NoCallers]]), it is not reachable, and
     * its declaring class will not be loaded (at least not via this call).
     *
     * If it is not yet known, we register a dependency to it.
     *
     * In case there are definitively some callers, we remove the potential existing dependency
     * and handle the method being newly reachable (i.e. analyse the field accesses of the method
     * and update its declaring class type as reachable)
     *
     * @return the static initializers, that are definitively not yet processed by the call graph
     *         analysis and became reachable here.
     */
    def handleCaller(
        state: LoadedClassesState, callersOfMethod: EOptionP[DeclaredMethod, CallersProperty]
    ): Traversable[DeclaredMethod] = {
        var reachableMethods = List.empty[DeclaredMethod]
        callersOfMethod match {
            case FinalEP(_, NoCallers) ⇒
                // the method was not called at all
                state.removeDependee(callersOfMethod.toEPK)
            case _: EPK[DeclaredMethod, CallersProperty] ⇒
                // there is no result for this method available yet.
                // here we assume that the dependency was not yet registered or a set is used
                // as underlying data structure
                state.addDependee(callersOfMethod)
            case eps @ EPS(_, _, NoCallers) ⇒
                // the method may have no callers, or may be reachable.
                // we do not know, so keep the dependency alive (up to date)
                state.updateDependee(eps)
            case eps @ EPS(dm: DeclaredMethod, _, _) ⇒
                // the method has callers. we have to analyze it and the dependency can be removed
                state.removeDependee(eps.toEPK)
                val newReachableMethods = handleNewReachableMethod(dm, state)
                reachableMethods ++= newReachableMethods
        }
        reachableMethods
    }

    /**
     * For a reachable method, its declaring class will be loaded by the VM (if not done already).
     * In order to ensure this, the `state` will be updated.
     *
     * Furthermore, the method may access static fields, which again may lead to class loading.
     *
     * @return the static initializers, that became reachable and were not yet processed by the
     *         call graph analysis.
     *
     */
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
                case PutStatic(_, dct, _, _, _) if !state.loadedClasses.contains(dct) ⇒
                    handleType(dct).foreach(newReachableMethods +:= _)
                    state.addLoadedType(dct)
                case Assignment(_, _, GetField(_, dct, _, _, _)) if !state.loadedClasses.contains(dct) ⇒
                    handleType(dct).foreach(newReachableMethods +:= _)
                    state.addLoadedType(dct)
                case _ ⇒
            }
        }

        newReachableMethods
    }

    /**
     * Checks, whether the static initializer of the given type exisits and were not already
     * processed by the `callGraphAlgorithm`.
     * In this case it registers it for being processed and returns it as option.
     * Otherwise it returns None.
     */
    def handleType(declaringClassType: ObjectType): Option[DefinedMethod] = {
        project.classFile(declaringClassType).flatMap { cf ⇒
            cf.staticInitializer.flatMap { clInit ⇒
                val clInitDM = declaredMethods(clInit)
                // only if registerMethodToProcess returns true, we have Some(<clinit>)
                // i.e. the call graph analysis has not already processed this method
                // (after a successful registration, a second call will return false)
                Some(clInitDM).filter(callGraphAnalysis.registerMethodToProcess)
            }
        }
    }

    /**
     * Returns an [[IncrementalResult]], which let the newly reachable methods being analyzed
     * by the call graph analysis. This result contains the result for the loaded classes
     * (intermediate if there are open dependencies)
     */
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

    /**
     * Just handles the update according to the `handleCaller` method and `returnResult`
     */
    def continuation(state: LoadedClassesState)(eps: SomeEPS): PropertyComputationResult = {
        val reachableMethods =
            handleCaller(state, eps.asInstanceOf[EPS[DeclaredMethod, CallersProperty]])

        returnResult(reachableMethods, state)
    }
}

object EagerLoadedClassesAnalysis extends FPCFEagerAnalysisScheduler {

    override def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new LoadedClassesAnalysis(project, null)
        propertyStore.scheduleEagerComputationsForEntities(List(project))(analysis.doAnalyze)
        analysis
    }

    override def uses: Predef.Set[PropertyKind] = Predef.Set(CallersProperty)

    override def derives: Predef.Set[PropertyKind] = Predef.Set(LoadedClasses)
}
