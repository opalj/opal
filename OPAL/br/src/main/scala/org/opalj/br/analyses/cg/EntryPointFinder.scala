/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import org.opalj.log.LogContext
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
trait EntryPointFinder {

    /**
     * Returns the set of entry points for a given project under analysis. Entry points may be virtual, if they are
     * defined by the configuration but not contained in the project.
     *
     * @param project The concrete project for which to compute entry points
     * @return Set of entry points to the project
     */
    def collectEntryPoints(project: SomeProject): Iterable[DeclaredMethod] = Set.empty[DeclaredMethod]

    /**
     * Returns ProjectInformationKeys required by this EntryPointFinder
     * If no extra keys are required, `Nil` can be returned.
     *
     * @param project The concrete project for which to compute requirements
     * @return The set of required project information key
     */
    def requirements(project: SomeProject): ProjectInformationKeys = Seq(DeclaredMethodsKey)
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

    override def collectEntryPoints(project: SomeProject): Iterable[DeclaredMethod] = {

        val declaredMethods = project.get(DeclaredMethodsKey)

        val MAIN_METHOD_DESCRIPTOR = MethodDescriptor.JustTakes(FieldType.apply("[Ljava/lang/String;"))

        super.collectEntryPoints(project) ++ project.allMethodsWithBody.iterator.collect {
            case m: Method
                if m.isStatic
                    && (m.descriptor == MAIN_METHOD_DESCRIPTOR)
                    && (m.name == "main") =>
                declaredMethods(m)
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
    private val packagesToExclude = Set("com/sun", "sun", "oracle", "jdk", "java", "com/oracle", "javax", "sunw")

    override def collectEntryPoints(project: SomeProject): Iterable[DeclaredMethod] = {
        super.collectEntryPoints(project).filterNot { ep =>
            packagesToExclude.exists { prefix =>
                ep.declaringClassType.packageName.startsWith(prefix) &&
                ep.name == "main"
            }
        }.filterNot { ep =>
            // The WrapperGenerator class file is part of the rt.jar in 1.7., but is in the
            // default package.
            ep.declaringClassType == ClassType("WrapperGenerator")
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

    override def collectEntryPoints(project: SomeProject): Iterable[DeclaredMethod] = {
        val declaredMethods = project.get(DeclaredMethodsKey)

        val isClosedPackage = project.get(ClosedPackagesKey).isClosed _
        val isExtensible = project.get(TypeExtensibilityKey)
        val classHierarchy = project.classHierarchy

        @inline def isEntryPoint(method: Method): Boolean = {
            val classFile = method.classFile
            val ct = classFile.thisType

            if (isClosedPackage(ct.packageName)) {
                if (method.isPublic) {
                    classHierarchy.allSubtypes(ct, reflexive = true).exists { st =>
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
                                m => m.isStatic && m.isPublic && m.returnType == ct
                            })
                    }
                } else if (method.isProtected) {
                    isExtensible(ct).isYesOrUnknown &&
                    (method.isStatic ||
                    classHierarchy.allSubtypes(ct, reflexive = true).exists { st =>
                        project.classFile(st).forall(_.constructors.exists { c => c.isPublic || c.isProtected })
                    })
                } else false
            } else {
                // all methods in an open package are accessible
                !method.isPrivate
            }
        }

        val eps = project
            .allMethodsWithBody
            .iterator
            .collect {
                case m if isEntryPoint(m) =>
                    declaredMethods(m)
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
        InitialEntryPointsKey.ConfigKeyPrefix + "entryPoints"
    }

    override def collectEntryPoints(project: SomeProject): Iterable[DeclaredMethod] = {
        import net.ceedubs.ficus.readers.ArbitraryTypeReader._

        implicit val logContext: LogContext = project.logContext

        val declaredMethods = project.get(DeclaredMethodsKey)

        var entryPoints = Set.empty[DeclaredMethod]

        if (!project.config.hasPath(additionalEPConfigKey)) {
            OPALLogger.info(
                "project configuration",
                s"configuration key $additionalEPConfigKey is missing; " +
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
                        s"configuration key $additionalEPConfigKey is invalid; " +
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

            val classType = ClassType(typeName)
            val methodDescriptor: Option[MethodDescriptor] = descriptor.flatMap { md =>
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
            }

            def findMethods(classType: ClassType, isSubtype: Boolean = false): Unit = {
                project.classFile(classType) match {
                    case Some(cf) =>
                        var methods: List[Method] = cf.findMethod(name)

                        if (methods.isEmpty)
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
                                    s"$typeName does not define a method $name(${md.toJVMDescriptor}); " +
                                        "entry point ignored"
                                )
                        }

                        entryPoints = entryPoints ++ methods.map(declaredMethods.apply).toSet

                    case None if !isSubtype =>

                        if (methodDescriptor.isDefined) {
                            val virtualEntry =
                                declaredMethods(classType, classType.packageName, classType, name, methodDescriptor.get)

                            entryPoints = entryPoints + virtualEntry

                            OPALLogger.info(
                                "project configuration",
                                s"the declaring class $typeName of the entry point has not been found, using virtual method ${virtualEntry.toString} as entry"
                            )
                        }

                    case None => throw new MatchError(None) // TODO: Pattern match not exhaustive
                }

            }

            findMethods(classType)
            if (considerSubtypes) {
                project.classHierarchy.allSubtypes(classType, reflexive = false).foreach {
                    ct => findMethods(ct, isSubtype = true)
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
        InitialEntryPointsKey.ConfigKeyPrefix + "AllEntryPointsFinder.projectMethodsOnly"

    override def collectEntryPoints(project: SomeProject): Iterable[DeclaredMethod] = {
        val declaredMethods = project.get(DeclaredMethodsKey)

        if (project.config.as[Boolean](ConfigKey))
            project.allProjectClassFiles.flatMap(_.methodsWithBody.map(declaredMethods.apply))
        else project.allMethodsWithBody.map(declaredMethods.apply)
    }
}
