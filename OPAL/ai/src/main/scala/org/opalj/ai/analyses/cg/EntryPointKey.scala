/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package ai
package analyses
package cg

import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.InstantiableClassesKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.analysis.EntryPointsAnalysis
import org.opalj.fpcf.properties.IsEntryPoint
import org.opalj.log.OPALLogger

/**
 * The ''key'' object to get the entry point of a project. The entry points are computed w.r.t. to the analysis mode
 * of the project.
 *
 * You can assume that – in general – the call graph calculated using the VTA algorithm
 * is more precise than the call graph calculated using the CHA algorithm. Depending
 * on the project, the performance may be better, equal or worse.
 *
 * @example
 *      To get the call graph object use the `Project`'s `get` method and pass in
 *      `this` object.
 *      {{{
 *      val EntryPointInformation = project.get(EntryPointKey)
 *      }}}
 * @author Michael Reif
 */
object EntryPointKey extends ProjectInformationKey[EntryPointInformation] {

    override protected def requirements = Seq(
        FPCFAnalysesManagerKey,
        SourceElementsPropertyStoreKey,
        InstantiableClassesKey
    )

    override protected def compute(project: SomeProject): EntryPointInformation = {

        val fpcfManager = project.get(FPCFAnalysesManagerKey)
        if (!fpcfManager.isDerived(EntryPointsAnalysis.derivedProperties))
            fpcfManager.runWithRecommended(EntryPointsAnalysis)(true)
        else
            OPALLogger.warn("analysis", "Entry points were already computed; The already available entry points are used.")(project.logContext)

        val configuredEntryPoints = getConfigEntryPoints(project)
        new EntryPointInformation(project, configuredEntryPoints)
    }

    def getConfigEntryPoints(project: SomeProject): Set[Method] = {
        import net.ceedubs.ficus.Ficus._
        import net.ceedubs.ficus.readers.ArbitraryTypeReader._

        if (!project.config.hasPath("org.opalj.callgraph.entryPoints")) {
            OPALLogger.error("project config", "no config entry for additional entry points has been found")(project.logContext)
            return Set.empty;
        }

        val configEntryPoints = project.config.as[List[EntryPoint]]("org.opalj.callgraph.entryPoints")
        (configEntryPoints map { ep ⇒
            val EntryPoint(epDeclaringClass, epName, epDescriptor) = ep
            val ot = ObjectType(epDeclaringClass)
            project.classFile(ot) match {
                case Some(cf) ⇒
                    if (epDescriptor.isEmpty)
                        cf.methods.filter { method ⇒ method.name == epName }
                    else {
                        val md = MethodDescriptor(ep.descriptor.get)
                        cf.methods.filter { method ⇒
                            (method.descriptor == md) && (method.name == epName)
                        }
                    }
                case None ⇒
                    IndexedSeq.empty[Method]
            }
        }).foldLeft(Set.empty[Method])((res, methods) ⇒ res ++ methods)
    }
}

/* need by the ArbitraryTypeReader of ficus */
case class EntryPoint private (val declaringClass: String, name: String, descriptor: Option[String])

class EntryPointInformation(
        project:               SomeProject,
        configuredEntryPoints: Set[Method]
) {

    def getEntryPoints(): Set[Method] = {
        getEntryPointsFromPropertyStore(project) ++ configuredEntryPoints
    }

    /*
     * Get all methods from the property store that are entry points.
     */
    private[this] def getEntryPointsFromPropertyStore(project: SomeProject): Set[Method] = {
        val propertyStore = project.get(SourceElementsPropertyStoreKey)
        propertyStore.collect { case (m: Method, IsEntryPoint) if m.body.nonEmpty ⇒ m }.toSet
    }
}

