/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import scala.collection.mutable.ArrayBuffer
import org.opalj.log.OPALLogger
import net.ceedubs.ficus.Ficus._

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
    def collectEntryPoints(project: SomeProject): Iterable[Method] = Set.empty[Method]
}

/**
 * This trait provides an analysis to compute the entry points of a standard command-line
 * application. Please note that a command-line application can provide multiple entry points. This
 * analysis identifies **all** main methods of the given code.
 *
 * @note If it is required to find only a specific main method as entry point, please use the
 *       configuration-based entry point finder.
 *
 * @author Michael Reif
 */
trait ApplicationEntryPointsFinder extends EntryPointFinder {

    override def collectEntryPoints(project: SomeProject): Iterable[Method] = {
        val MAIN_METHOD_DESCRIPTOR = MethodDescriptor.JustTakes(FieldType.apply("[Ljava/lang/String;"))

        super.collectEntryPoints(project) ++ project.allMethodsWithBody.collect {
            case m: Method if m.isStatic
                && (m.descriptor == MAIN_METHOD_DESCRIPTOR)
                && (m.name == "main") => m
        }
    }
}

/**
 * Similar to the [[ApplicationEntryPointsFinder]] this trait selects main methods as entry points
 * for standard command line applications. It excludes, however, main methods found in the Java
 * Runtime Environment to prevent analyses of applications where the JRE is included in the project
 * from being polluted by these entry points that are not part of the application.
 *
 * @author Florian Kuebler
 */
trait ApplicationWithoutJREEntryPointsFinder extends ApplicationEntryPointsFinder {
    private val packagesToExclude = Set(
        "com/sun", "sun", "oracle", "jdk", "java", "com/oracle", "javax", "sunw"
    )

    override def collectEntryPoints(project: SomeProject): Iterable[Method] = {
        super.collectEntryPoints(project).filterNot { ep =>
            packagesToExclude.exists { prefix =>
                ep.declaringClassFile.thisType.packageName.startsWith(prefix)
            }
        }.filterNot { ep =>
            // The WrapperGenerator class file is part of the rt.jar in 1.7., but is in the
            // default package.
            ep.classFile.thisType == ObjectType("WrapperGenerator")
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

    override def collectEntryPoints(project: SomeProject): Iterable[Method] = {
        val isClosedPackage = project.get(ClosedPackagesKey).isClosed _
        val isExtensible = project.get(TypeExtensibilityKey)
        val classHierarchy = project.classHierarchy

        @inline def isEntryPoint(method: Method): Boolean = {
            val classFile = method.classFile
            val ot = classFile.thisType

            if (isClosedPackage(ot.packageName)) {
                if (method.isPublic) {
                    classHierarchy.allSubtypes(ot, reflexive = true).exists { st =>
                        val subtypeCFOption = project.classFile(st)
                        // Class file must be public to access it
                        subtypeCFOption.forall(_.isPublic) &&
                            // Method must be static or class instantiable
                            (method.isStatic ||
                                // Note: This is not enough to ensure that the type is instantiable
                                // (supertype might have no accessible constructor),
                                // but it soundly overapproximates
                                subtypeCFOption.forall(_.constructors.exists { c =>
                                    c.isPublic || (c.isProtected && isExtensible(st).isYesOrUnknown)
                                }) || classFile.methods.exists {
                                    m => m.isStatic && m.isPublic && m.returnType == ot
                                })
                    }
                } else if (method.isProtected) {
                    isExtensible(ot).isYesOrUnknown &&
                        (method.isStatic ||
                            classHierarchy.allSubtypes(ot, reflexive = true).exists { st =>
                                project.classFile(st).forall(_.constructors.exists { c =>
                                    c.isPublic || c.isProtected
                                })
                            })
                } else false
            } else {
                // all methods in an open package are accessible
                !method.isPrivate
            }
        }

        val eps = ArrayBuffer.empty[Method]

        project.allMethodsWithBody.foreach { method =>
            if (isEntryPoint(method))
                eps.append(method)
        }
        super.collectEntryPoints(project) ++ eps
    }
}

/**
 * This trait provides an analysis that loads entry points from the project configuration file.
 *
 * All entry points must be configured under the following configuration key:
 *      **org.opalj.br.analyses.cg.InitialEntryPointsKey.entryPoints**
 *
 * Example:
 * {{{
 *        org.opalj.br.analyses.cg {
 *            InitialEntryPointKey {
 *                analysis = "org.opalj.br.analyses.cg.ConfigurationEntryPointsFinder"
 *                entryPoints = [
 *                  {declaringClass = "java/util/List+", name = "add"},
 *                  {declaringClass = "java/util/List", name = "remove", descriptor = "(I)Z"}
 *                ]
 *            }
 *        }
 *  }}}
 *
 * Please note that the first entry point, by adding the "+" to the declaring class' name, considers
 * all "add" methods from all subtypes independently from the respective method's descriptor. In
 * contrast, the second entry does specify a descriptor and does not consider List's subtypes (by
 * not suffixing a plus to the declaringClass) which implies that only the remove method with this
 * descriptor is considered as entry point.
 *
 * @author Michael Reif
 */
trait ConfigurationEntryPointsFinder extends EntryPointFinder {

    // don't make this a val for initialization reasons
    @inline private[this] def additionalEPConfigKey: String = {
        InitialEntryPointsKey.ConfigKeyPrefix+"entryPoints"
    }

    override def collectEntryPoints(project: SomeProject): Iterable[Method] = {
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
                case e: Throwable =>
                    OPALLogger.error(
                        "project configuration - recoverable",
                        s"configuration key $additionalEPConfigKey is invalid; "+
                            "see EntryPointKey documentation",
                        e
                    )
                    return entryPoints;
            }

        configEntryPoints foreach { ep =>
            val EntryPointContainer(configuredType, name, descriptor) = ep

            OPALLogger.debug("project configuration - entry points", ep.toString)

            val considerSubtypes = configuredType.endsWith("+")
            val typeName = if (considerSubtypes) {
                configuredType.substring(0, configuredType.size - 1)
            } else {
                configuredType
            }

            val objectType = ObjectType(typeName)
            val methodDescriptor: Option[MethodDescriptor] = descriptor.map { md =>
                try {
                    Some(MethodDescriptor(md))
                } catch {
                    case _: IllegalArgumentException =>
                        OPALLogger.warn(
                            "project configuration",
                            s"illegal method descriptor: $typeName { $name or ${md}}"
                        )
                        None
                }
            }.getOrElse(None)

            def findMethods(objectType: ObjectType, isSubtype: Boolean = false): Unit = {
                project.classFile(objectType) match {
                    case Some(cf) =>
                        var methods: List[Method] = cf.findMethod(name)

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

                        if (methods.exists(_.body.isEmpty)) {
                            OPALLogger.warn(
                                "project configuration",
                                s"$typeName has an empty method $name); "+
                                    "entry point ignored"
                            )
                            methods = methods.filter(_.body.isDefined)
                        }

                        entryPoints = entryPoints ++ methods

                    case None if !isSubtype =>
                        OPALLogger.warn(
                            "project configuration",
                            s"the declaring class $typeName of the entry point has not been found"
                        )

                    case None => throw new MatchError(None) // TODO: Pattern match not exhaustive
                }

            }

            findMethods(objectType)
            if (considerSubtypes) {
                project.classHierarchy.allSubtypes(objectType, false).foreach {
                    ot => findMethods(ot, true)
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

/**
 * The ConfigurationEntryPointsFinder considers only configured entry points.
 *
 * @author Dominik Helm
 */
object ConfigurationEntryPointsFinder
    extends ConfigurationEntryPointsFinder

/**
 * The ApplicationEntryPointsFinder considers all main methods plus additionally configured entry points.
 *
 * @author Michael Reif
 */
object ApplicationEntryPointsFinder
    extends ApplicationEntryPointsFinder
    with ConfigurationEntryPointsFinder

object ApplicationWithoutJREEntryPointsFinder
    extends ApplicationWithoutJREEntryPointsFinder
    with ConfigurationEntryPointsFinder

/**
 * The ApplicationEntryPointsFinder considers all main methods plus additionally configured entry points.
 *
 * @author Michael Reif
 */
object LibraryEntryPointsFinder
    extends LibraryEntryPointsFinder
    with ConfigurationEntryPointsFinder

/**
 * The MetaEntryPointsFinder is a conservative EntryPoints finder triggers all known finders.
 *
 * @author Michael Reif
 */
object MetaEntryPointsFinder
    extends ApplicationEntryPointsFinder
    with LibraryEntryPointsFinder
    with ConfigurationEntryPointsFinder

/**
 * The AllEntryPointsFinder considers all methods as entry points. It can be configured to consider
 * only project methods as entry points instead of project and library methods by specifying
 * **org.opalj.br.analyses.cg.InitialEntryPointsKey.AllEntryPointsFinder.projectMethodsOnly=true**.
 *
 * @author Dominik Helm
 */
object AllEntryPointsFinder extends EntryPointFinder {
    final val ConfigKey =
        InitialEntryPointsKey.ConfigKeyPrefix+"AllEntryPointsFinder.projectMethodsOnly"

    override def collectEntryPoints(project: SomeProject): Iterable[Method] = {
        if (project.config.as[Boolean](ConfigKey))
            project.allProjectClassFiles.flatMap(_.methodsWithBody)
        else project.allMethodsWithBody
    }
}