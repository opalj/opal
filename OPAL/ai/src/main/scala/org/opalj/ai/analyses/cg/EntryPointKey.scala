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
 * Please note that the automatic entry point analysis is not sufficient when it comes for example to system
 * callbacks. Those calls are implicitly triggered by the JVM, a custom framework, a custom web
 * server or others. To overcome that limitation, the key provides a mechanism to specify individual entry points
 * via the configuration file. To use that mechanism, it's required to add the following config key
  * to the configuration file. More details are below:
 *
 * The general format of the key that can be added to the application.conf or reference.conf.
 *
 * {{{
 *   org.opalj.callgraph.entryPoints = [
 *     {declaringClass = "<declClass>",
 *      methodName = "name",
 *      descriptor = "<desc in JVM notation>"} # the descriptor is optional
 *   ]
 * }}}
 *
 * As the previous definition suggests each entry point definition consists of the obligatory parameters
 * `declaringClass` and `name`. The optional third parameter is the method descriptor.
 *
 * @example
 *     Specify a special-,system,- or framework-specific entry point within the application.conf.
 *     The example defines at least two methods as entry point:
 *       - all methods in "com.test.Main" with the name "main", i.e. if more than one method named
 *         "main" exists all are added.
 *       - the method in "com.test.Main" with the name "increase" that requires an integer parameter
 *         and also return an integer. If a method descriptor is given, duplicates can't exist.
 *
 * {{{
 *   org.opalj.callgraph.entryPoints = [
 *        { declaringClass = "com/test/Main",
 *          name = "main"},
 *        { declaringClass = "com/test/Main",
 *          name = "increase",
 *          descriptor = "(I)I" # optional
 *         }]
 * }}}
 *
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

        implicit val logContext = project.logContext

        if (!project.config.hasPath("org.opalj.callgraph.entryPoints")) {
            OPALLogger.info(
                "config file",
                "None additional entry points configured - :"+"""key: "org.opalj.callgraph.entryPoints" is missing."""
            )

            return Set.empty;
        }

        var configEntryPoints: Option[List[EntryPoint]] = None

        try {
            val eps = project.config.as[List[EntryPoint]]("org.opalj.callgraph.entryPoints")
            configEntryPoints = Some(eps)
        } catch {
            case e: Throwable ⇒ OPALLogger.error(
                "config",
                "Malformed configuration at key: 'org.opalj.callgraph.entryPoints'."+
                    "Please find examples at the EntryPointKey documentation. - Configuration ignored."
            )
        }

        var entryPoints = Set.empty[Method]

        if (configEntryPoints.nonEmpty) {
            configEntryPoints.get map { ep ⇒
                val EntryPoint(declClass, name, descriptor) = ep
                val ot = ObjectType(declClass)

                project.classFile(ot) match {
                    case Some(cf) ⇒
                        var methods = cf.findMethod(name)

                        if (methods.size == 0)
                            OPALLogger.warn("config", s"No entry point method with the name: $name has been found in: $declClass")

                        if (descriptor.nonEmpty) {
                            try {
                                val methodDescriptor = MethodDescriptor(descriptor.get)
                                methods = methods.filterNot(_.descriptor == methodDescriptor)

                                if (methods.size > 1)
                                    OPALLogger.warn("config", s"No entry point method with the name: $name and ${descriptor.get} has been found in: $declClass")
                            } catch {
                                case e: IllegalArgumentException ⇒
                                    OPALLogger.warn("config", s"Illegal method descriptor at specified entry point method at: ($declClass, $name, ${descriptor.get})")
                            }
                        }

                        entryPoints = entryPoints ++ methods

                    case None ⇒
                        OPALLogger.warn("config", s"Declaring class of the entry point has not been found: $declClass")
                }
            }
        }

        entryPoints
    }
}

/* need by the ArbitraryTypeReader of ficus */
private case class EntryPoint private (val declaringClass: String, name: String, descriptor: Option[String])

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

