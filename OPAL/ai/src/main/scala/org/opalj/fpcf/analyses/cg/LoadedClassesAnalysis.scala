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
import org.opalj.fpcf.properties.LowerBoundCallers
import org.opalj.fpcf.properties.NoCallers
import org.opalj.fpcf.properties.OnlyVMLevelCallers
import org.opalj.tac.Assignment
import org.opalj.tac.GetField
import org.opalj.tac.PutStatic
import org.opalj.tac.SimpleTACAIKey

/**
 * Computes the set of classes that are being loaded by the VM during the execution of the
 * `project`.
 * @author Florian Kuebler
 */
class LoadedClassesAnalysis(
        val project: SomeProject
) extends FPCFAnalysis {
    private val tacaiProvider = project.get(SimpleTACAIKey)
    private val declaredMethods = project.get(DeclaredMethodsKey)

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

        PartialResult[SomeProject, LoadedClasses](project, LoadedClasses.key, {
            case EPK(p, _) ⇒ Some(EPS(p, null /*TODO LB*/ , new LoadedClasses(UIDSet.empty)))
            case _         ⇒ None
        })
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
        declaredMethod: DeclaredMethod
    ): PropertyComputationResult = {
        val callersOfMethod = propertyStore(declaredMethod, CallersProperty.key)
        callersOfMethod match {
            case FinalEP(_, NoCallers) ⇒
                // nothing to do, since there is no caller
                NoResult
            case EPK(_, _) ⇒
                throw new IllegalStateException("unexpected state")
            case EPS(_, _, NoCallers) ⇒
                // we can not create a dependency here, so the analysis is not allowed to create
                // such a result
                throw new IllegalStateException("illegal immediate result for callers")
            case eps @ EPS(dm: DeclaredMethod, _, _) ⇒
                // the method has callers. we have to analyze it
                val (newCLInits, newLoadedClasses) = handleNewReachableMethod(dm)

                if (newLoadedClasses.nonEmpty) {
                    PartialResult[SomeProject, LoadedClasses](project, LoadedClasses.key, {
                        case EPK(p, _) ⇒
                            throw new IllegalStateException("there should be already a result")
                        case EPS(_, _: LoadedClasses, ub: LoadedClasses) if newLoadedClasses.subsetOf(ub.classes) ⇒
                            None
                        case EPS(p, lb: LoadedClasses, ub: LoadedClasses) ⇒
                            Some(EPS(p, lb, new LoadedClasses(ub.classes ++ newLoadedClasses)))

                    })

                    newCLInits map { clInit ⇒
                        PartialResult[DeclaredMethod, CallersProperty](clInit, CallersProperty.key, {
                            case EPK(_, _) ⇒
                                Some(EPS(
                                    clInit, new LowerBoundCallers(project, clInit), OnlyVMLevelCallers
                                ))
                            case EPS(_, lb, ub) if !ub.hasCallersWithUnknownContext ⇒
                                Some(EPS(clInit, lb, ub.updateVMLevelCall()))
                            case _ ⇒ None
                        })
                    }
                } else {
                    NoResult
                }

                NoResult
        }
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
        dm: DeclaredMethod
    ): (Set[DeclaredMethod], UIDSet[ObjectType]) = {
        assert(dm.hasSingleDefinedMethod)
        val method = dm.definedMethod
        val methodDCT = method.classFile.thisType
        assert(dm.declaringClassType eq methodDCT)

        var newCLInits = Set.empty[DeclaredMethod]
        var newLoadedClasses = UIDSet.empty[ObjectType]

        // whenever a method is called the first time, its declaring class gets loaded
        handleType(method.classFile.thisType).foreach(newCLInits += _)
        newLoadedClasses += methodDCT

        for (stmt ← tacaiProvider(method).stmts) {
            stmt match {
                case PutStatic(_, dc, _, _, _) if !newLoadedClasses.contains(dc) ⇒
                    handleType(dc).foreach(newCLInits += _)
                    newLoadedClasses += dc
                case Assignment(_, _, GetField(_, dc, _, _, _)) if !newLoadedClasses.contains(dc) ⇒
                    handleType(dc).foreach(newCLInits += _)
                    newLoadedClasses += dc
                case _ ⇒
            }
        }

        (newCLInits, newLoadedClasses)
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
                Some(clInitDM)
            }
        }
    }
}

object EagerLoadedClassesAnalysis extends FPCFEagerAnalysisScheduler {

    override type InitializationData = LoadedClassesAnalysis

    override def start(
        project:               SomeProject,
        propertyStore:         PropertyStore,
        loadedClassesAnalysis: LoadedClassesAnalysis
    ): FPCFAnalysis = {
        propertyStore.scheduleEagerComputationsForEntities(List(project))(loadedClassesAnalysis.doAnalyze)
        loadedClassesAnalysis
    }

    override def uses: Predef.Set[PropertyKind] = Predef.Set(CallersProperty)

    override def derives: Predef.Set[PropertyKind] = Predef.Set(LoadedClasses)

    override def init(p: SomeProject, ps: PropertyStore): LoadedClassesAnalysis = {
        val analysis = new LoadedClassesAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.handleCaller)
        analysis
    }
}
