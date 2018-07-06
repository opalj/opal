/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2018
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
package br
package analyses
package cg

import scala.collection.mutable.ArrayBuffer
import org.opalj.collection.immutable.Chain
import org.opalj.log.OPALLogger
import net.ceedubs.ficus.Ficus._
import org.opalj.br.ObjectType

/**
 * The EntryPointFinder trait is a common trait for all analyses that can derive an programs entry
 * points. The concrete entry point finder that is used to determines a programs entry points directly
 * impacts the computation of a programs call graph.
 *
 * All subclasses should be implemented in a way that it is possible to chain them. (Decorator)
 *
 * @author Michael Reif
 */
sealed trait EntryPointFinder {

    /*
    * Returns the entry points with respect to a concrete scenario.
    *
    * This method must be implemented by any subtype.
    */
    def collectEntryPoints(project: SomeProject): Traversable[Method] = Set.empty[Method]
}

/**
 * This trait provides an analysis to compute the entry points of a standard command-line
 * application. Please note that a command-line application can provide multiple entry points. This
 * analysis identifies **all** main methods of the given code.
 *
 * @note If it is required to find only a specific main method as entry point, please use the
 *       configuration-based entry point finder.
 *
 *  @author Michael Reif
 */
trait ApplicationEntryPointsFinder extends EntryPointFinder {

    override def collectEntryPoints(project: SomeProject): Traversable[Method] = {
        val MAIN_METHOD_DESCRIPTOR = MethodDescriptor.JustTakes(FieldType.apply("[Ljava/lang/String;"))

        super.collectEntryPoints(project) ++ project.allMethodsWithBody.collect {
            case m: Method if m.isStatic
                && (m.descriptor == MAIN_METHOD_DESCRIPTOR)
                && (m.name == "main") ⇒ m
        }
    }
}

/**
 * This trait provides an analysis to compute a libraries' default entry points. The analysis thus
 * depends on the type extensibility, method overridability, and closed packages information. Hence,
 * its behaviour and output heavily depends on the configuration settings.
 *
 * @note If the target program relies on frameworks with additional custom entry points, you can
 *       combine this analysis with the additional configurable entry points.
 *
 * @author Michael Reif
 */
trait LibraryEntryPointsFinder extends EntryPointFinder {

    override def collectEntryPoints(project: SomeProject): Traversable[Method] = {
        val isClosedPackage = project.get(ClosedPackagesKey).isClosed _
        val isExtensible = project.get(TypeExtensibilityKey)
        val isOverridable = project.get(IsOverridableMethodKey)

        @inline def isEntryPoint(method: Method): Boolean = {
            val classFile = method.classFile
            val ot = classFile.thisType

            if (isClosedPackage(ot.packageName)) {
                if (method.isStatic) {
                    (classFile.isPublic && method.isPublic) ||
                        (method.isProtected && isExtensible(ot).isYesOrUnknown)
                } else if (method.isFinal) {
                    classFile.isPublic && method.isPublic
                } else {
                    isOverridable(method).isYesOrUnknown
                }
            } else {
                // all methods in an open package are accessible
                !method.isPrivate
            }
        }

        val eps = ArrayBuffer.empty[Method]

        project.allMethodsWithBody.foreach { method ⇒
            println(method.toJava)
            if (isEntryPoint(method))
                eps.append(method)
        }
        super.collectEntryPoints(project) ++ eps
    }
}

/**
 * This trait provides an analysis that loads entry points from the given project configuration file.
 *
 * All entry points must be configured under the following configuration key:
 *      **org.opalj.br.analyses.cg.InitialEntryPointsKey.entryPoints**
 *
 * Example:
 * {{{
 *        org.opalj.br.analyses {
 *            InitialEntryPointKey {
 *                analysis = "org.opalj.br.analyses.ConfigurationEntryPointsFinder"
 *                entryPoints = [
 *                     ("java/lang/List+", "add"),
 *                     ("java/lang/List", "remove, "(I)Z")
 *                ]
 *            }
 *        }
 *  }}}
 *
 */
trait ConfigurationEntryPointsFinder extends EntryPointFinder {

    // don't make this a val for initialization reasons
    @inline private[this] def additionalEPConfigKey: String = {
        InitialEntryPointsKey.ConfigKeyPrefix+"entryPoints"
    }

    override def collectEntryPoints(project: SomeProject): Traversable[Method] = {
        import net.ceedubs.ficus.readers.ArbitraryTypeReader._

        implicit val logContext = project.logContext
        var entryPoints = Set.empty[Method]

        if (!project.config.hasPath(additionalEPConfigKey)) {
            OPALLogger.info(
                "project configuration",
                s"configuration key $additionalEPConfigKey is missing; "+
                    "no additional entry points configured"
            )
            return entryPoints;
        }
        val configEntryPoints: List[EntryPointContainer] =
            try {
                project.config.as[List[EntryPointContainer]](additionalEPConfigKey)
            } catch {
                case e: Throwable ⇒
                    OPALLogger.error(
                        "project configuration - recoverable",
                        s"configuration key $additionalEPConfigKey is invalid; "+
                            "see EntryPointKey documentation",
                        e
                    )
                    return entryPoints;
            }

        configEntryPoints foreach { ep ⇒
            val EntryPointContainer(configuredType, name, descriptor) = ep

            val considerSubtypes = configuredType.endsWith("+")
            val typeName = if (considerSubtypes) {
                configuredType.substring(0, configuredType.size - 1)
            } else {
                configuredType
            }

            val objectType = ObjectType(typeName)
            val methodDescriptor: Option[MethodDescriptor] = descriptor.map { md ⇒
                try {
                    Some(MethodDescriptor(md))
                } catch {
                    case _: IllegalArgumentException ⇒
                        OPALLogger.warn(
                            "project configuration",
                            s"illegal method descriptor: $typeName { $name or ${md}}"
                        )
                        None
                }
            }.getOrElse(None)

            def findMethods(objectType: ObjectType, isSubtype: Boolean = false): Unit = {
                project.classFile(objectType) match {
                    case Some(cf) ⇒
                        var methods: Chain[Method] = cf.findMethod(name)

                        if (methods.size == 0)
                            OPALLogger.warn(
                                "project configuration",
                                s"$typeName does not define a method $name; entry point ignored"
                            )

                        if (methodDescriptor.nonEmpty) {
                            val md = methodDescriptor.get
                            methods = methods.filter(_.descriptor == md)

                            if (methods.isEmpty && !isSubtype)
                                OPALLogger.warn(
                                    "project configuration",
                                    s"$typeName does not define a method $name(${md.toJVMDescriptor}); "+
                                        "entry point ignored"
                                )
                        }

                        assert(methods.forall(_.body.isDefined), "A method without body has been identified as entry point")
                        entryPoints = entryPoints ++ methods

                    case None if !isSubtype ⇒
                        OPALLogger.warn(
                            "project configuration",
                            s"the declaring class $typeName of the entry point has not been found"
                        )
                }

            }

            findMethods(objectType)
            if (considerSubtypes) {
                project.classHierarchy.allSubtypes(objectType, false).foreach {
                    ot ⇒ findMethods(ot, true)
                }
            }

        }

        super.collectEntryPoints(project) ++ entryPoints
    }

    /* Required by Ficus' `ArbitraryTypeReader`*/
    private case class EntryPointContainer(
            declaringClass: String,
            name:           String,
            descriptor:     Option[String]
    )
}

object ApplicationEntryPointsFinder
    extends ApplicationEntryPointsFinder
    with ConfigurationEntryPointsFinder

object LibraryEntryPointsFinder
    extends LibraryEntryPointsFinder
    with ConfigurationEntryPointsFinder

/*
* The MetaEntryPointsFinder is a conservative EntryPoints finder triggers all known finders.
*/
object MetaEntryPointsFinder
    extends ApplicationEntryPointsFinder
    with LibraryEntryPointsFinder
    with ConfigurationEntryPointsFinder
