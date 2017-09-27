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
package ai
package analyses
package cg

import org.opalj.log.OPALLogger
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.InstantiableClassesKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.analyses.EntryPointsAnalysis
import org.opalj.fpcf.properties.IsEntryPoint
import org.opalj.fpcf.properties.EntryPoint
import org.opalj.fpcf.PropertyStore

/**
 * The ''key'' object to get the entry point(s) of a project. The entry points are computed w.r.t.
 * to the analysis mode of the project.
 *
 * @note    The entry point analysis is not sufficient when it comes – for example – to
 *          non-traceable callbacks; i.e., calls that are implicitly triggered by the JVM,
 *          a custom framework, a custom web server or others.
 *          To overcome that limitation, the key provides a mechanism to specify individual
 *          entry points via the configuration file. To use that mechanism, it's required to add
 *          the following config key to the configuration file.
 *          The general format of the JSON key that can be added to the `application.conf` or
 *          `reference.conf`.
 *          {{{
 *          org.opalj.callgraph.entryPoints = [
 *              {   declaringClass = "<name of the declaring class in JVM notation>",
 *                  methodName = "name",
 *                  descriptor = "<desciptor in JVM notation>" } # OPTIONAL
 *          ]
 *          }}}
 *          As the previous definition suggests, each entry point definition consists of the
 *          `declaringClass` and `name`. The optional third parameter is the method descriptor.
 *
 * @example The example defines at least two methods as entry points:
 *           - all methods in "com.test.Main" with the name "main", i.e. if more than one method
 *             named "main" exists all are added.
 *           - the method in "com.test.Main" with the name "increase" that requires an integer
 *             parameter and also return an integer. If a method descriptor is given, duplicates
 *             can't exist.
 *          {{{
 *          org.opalj.callgraph.entryPoints = [
 *              { declaringClass = "com/test/Main", name = "main" },
 *              { declaringClass = "com/test/Main", name = "increase", descriptor = "(I)I" }
 *          ]
 *          }}}
 *
 *
 * @example To get the entry point information use the `Project`'s `get` use:
 *          {{{
 *          val EntryPointInformation = project.get(EntryPointKey)
 *          }}}
 *
 * @author Michael Reif
 */
object EntryPointKey extends ProjectInformationKey[EntryPointInformation, Nothing] {

    override protected def requirements = Seq(
        FPCFAnalysesManagerKey,
        PropertyStoreKey,
        InstantiableClassesKey
    )

    override protected def compute(project: SomeProject): EntryPointInformation = {
        val fpcfManager = project.get(FPCFAnalysesManagerKey)
        if (!fpcfManager.isDerived(EntryPointsAnalysis.derivedProperties))
            fpcfManager.run(EntryPointsAnalysis)
        else
            OPALLogger.info(
                "analysis",
                "entry points were already computed; the already available entry points are used"
            )(project.logContext)

        val configuredEntryPoints = getConfigEntryPoints(project)
        new EntryPointInformation(project.get(PropertyStoreKey), configuredEntryPoints)
    }

    def getConfigEntryPoints(project: SomeProject): Set[Method] = {
        import net.ceedubs.ficus.Ficus._
        import net.ceedubs.ficus.readers.ArbitraryTypeReader._

        implicit val logContext = project.logContext
        var entryPoints = Set.empty[Method]

        if (!project.config.hasPath("org.opalj.callgraph.entryPoints")) {
            OPALLogger.info(
                "project configuration",
                "configuration key org.opalj.callgraph.entryPoints is missing; "+
                    "no additional entry points configured"
            )
            return entryPoints;
        }
        val configEntryPoints: List[EntryPointContainer] =
            try {
                project.config.as[List[EntryPointContainer]]("org.opalj.callgraph.entryPoints")
            } catch {
                case e: Throwable ⇒
                    OPALLogger.error(
                        "project configuration - recoverable",
                        "configuration key org.opalj.callgraph.entryPoints is invalid; "+
                            "see EntryPointKey documentation",
                        e
                    )
                    return entryPoints;
            }

        configEntryPoints foreach { ep ⇒
            val EntryPointContainer(declClass, name, descriptor) = ep

            project.classFile(ObjectType(declClass)) match {
                case Some(cf) ⇒
                    var methods = cf.findMethod(name)

                    if (methods.size == 0)
                        OPALLogger.warn(
                            "project configuration",
                            s"$declClass does not define a method $name; entry point ignored"
                        )

                    if (descriptor.nonEmpty) {
                        val jvmDescriptor = descriptor.get
                        try {
                            val methodDescriptor = MethodDescriptor(jvmDescriptor)
                            methods = methods.filter(_.descriptor == methodDescriptor)

                            if (methods.isEmpty)
                                OPALLogger.warn(
                                    "project configuration",
                                    s"$declClass does not define a method $name($jvmDescriptor); "+
                                        "entry point ignored"
                                )
                        } catch {
                            case e: IllegalArgumentException ⇒
                                OPALLogger.warn(
                                    "project configuration",
                                    s"illegal: $declClass or $name or $jvmDescriptor"
                                )
                        }
                    }

                    entryPoints = entryPoints ++ methods

                case None ⇒
                    OPALLogger.warn(
                        "project configuration",
                        s"the declaring class $declClass of the entry point has not been found"
                    )
            }

        }

        entryPoints
    }
}

/* Needed by the `ArbitraryTypeReader` of ficus. */
case class EntryPointContainer(
        declaringClass: String,
        name:           String,
        descriptor:     Option[String]
)

class EntryPointInformation(propertyStore: PropertyStore, configuredEntryPoints: Set[Method]) {

    def isEntryPoint(method: Method): Boolean = {
        val ep = propertyStore(method, EntryPoint.Key)
        if (ep.hasProperty) {
            ep.p == IsEntryPoint
        } else {
            false
        }
    }

    def foreach[U](f: Method ⇒ U): Unit = {
        propertyStore.entities(IsEntryPoint).foreach(e ⇒ f(e.asInstanceOf[Method]))
    }

    // LEGACY INTERFACE

    def getEntryPoints(): Set[Method] = {
        configuredEntryPoints ++
            propertyStore.collect { case (m: Method, IsEntryPoint) if m.body.nonEmpty ⇒ m }
    }
}
