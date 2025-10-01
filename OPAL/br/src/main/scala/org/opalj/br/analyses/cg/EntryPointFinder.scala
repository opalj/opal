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
 * According to the JVM 25 specification, valid main methods must be named "main", must return void
 * and may not be private. They must have either one parameter, which must be an array of strings,
 * or no parameters at all. Main methods can be inherited from interfaces or superclasses. The old
 * semantics before Java 25 can be applied by setting the following configuration value to false:
 *
 *  org.opalj.br.analyses.cg.InitialEntryPointsKey.useJava25Semantics
 *
 * @note If it is required to find only a specific main method as entry point, please use the
 *       configuration-based entry point finder.
 *
 * @author Johannes Düsing
 */
trait ApplicationEntryPointsFinder extends EntryPointFinder {

    private val java25ConfigKey = "org.opalj.br.analyses.cg.InitialEntryPointsKey.useJava25Semantics"

    override def collectEntryPoints(project: SomeProject): Iterable[DeclaredMethod] = {

        val useJava25Semantics = project.config.getBoolean(java25ConfigKey)

        val declaredMethods = project.get(DeclaredMethodsKey)

        val MAIN_METHOD_DESCRIPTOR_WITH_ARGS = MethodDescriptor.JustTakes(ArrayType(ClassType.String))
        val MAIN_METHOD_DESCRIPTOR_WITHOUT_ARGS = MethodDescriptor.NoArgsAndReturnVoid

        def isMainMethodWithArgs(m: Method): Boolean = m.name == "main" &&
            !m.isPrivate && m.descriptor == MAIN_METHOD_DESCRIPTOR_WITH_ARGS

        def isMainMethodWithoutArgs(m: Method): Boolean = m.name == "main" &&
            !m.isPrivate && m.descriptor == MAIN_METHOD_DESCRIPTOR_WITHOUT_ARGS

        if (useJava25Semantics) {
            val allEntryPoints = project.allClassFiles.flatMap { cf =>
                // Note that static methods are NOT contained in the instance methods - they must be looked up on the
                // class file's method definitions.
                val classTypeInstanceMethods = project.instanceMethods(cf.thisType)

                // For any given class, a main method with the string array parameter takes precedence over the one with
                // no parameters, as per JLS 25 §12.1.4 (and JVM Specification 25 §5.2).

                // We start by looking up main methods with the string array parameter that are defined by the current
                // class itself - this includes static method definitions.
                val theMainMethodOpt = cf.methods.find(isMainMethodWithArgs)
                    // If no matching method is found, we look for inherited main methods with string array parameter
                    .orElse(classTypeInstanceMethods.find(mDecl => isMainMethodWithArgs(mDecl.method)).map(_.method))
                    // If no matching method is found, we look for direct definitions of no parameter main methods
                    .orElse(cf.methods.find(isMainMethodWithoutArgs))
                    // If no matching method is found, we look for inherited main methods with no parameters
                    .orElse(classTypeInstanceMethods.find(mDecl => isMainMethodWithoutArgs(mDecl.method)).map(_.method))

                // At this point, theMainMethodOpt contains the main method with the highest precedence for the current
                // class, or None if no main method exists.

                // If the selected main method is not static, the JVM will invoke a default constructor for the current
                // class before invoking the main method itself. This constructor must also be treated as an entry point.
                if (theMainMethodOpt.isDefined && !theMainMethodOpt.get.isStatic) {

                    // We search for a default constructor
                    val defaultConstructor = cf
                        .methods
                        .find(m => m.isConstructor && m.descriptor == MethodDescriptor.NoArgsAndReturnVoid)

                    // If no default constructor exists, we cannot consider the selected main method - as per the JVM 25
                    // specification the execution will fail if no default constructor is available
                    if (defaultConstructor.isEmpty) Seq.empty
                    else Seq(theMainMethodOpt.get, defaultConstructor.get)
                } else {
                    theMainMethodOpt.toSeq
                }

            }

            super.collectEntryPoints(project) ++ allEntryPoints.map(declaredMethods.apply)
        } else {
            // Before Java 25, entry points must be methods named "main" that are public and static, and have one
            // parameter of type String[].
            super.collectEntryPoints(project) ++ project.allMethodsWithBody.iterator.collect {
                case m: Method
                    if m.isStatic
                        && (m.descriptor == MAIN_METHOD_DESCRIPTOR_WITH_ARGS)
                        && (m.name == "main")
                        && m.isPublic =>
                    declaredMethods(m)
            }
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
            ep.declaringClassType == ClassType.WrapperGenerator
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
