/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import net.ceedubs.ficus.Ficus._
import org.opalj.log.OPALLogger

/**
 *
 * @author Florian Kuebler
 * @author Michael Reif
 */
sealed trait InstantiatedTypesFinder {

    def collectInstantiatedTypes(project: SomeProject): Iterable[ObjectType] = {
        // These types can be introduced via constants!
        Iterable(ObjectType.String, ObjectType.Class)
    }
}

/**
 * This trait considers only the type java.lang.String as instantiated that is the type that is
 * be instantiated for command line applications for their command line parameters.
 *
 * @author Florian Kuebler
 */
trait ApplicationInstantiatedTypesFinder extends InstantiatedTypesFinder {

    override def collectInstantiatedTypes(project: SomeProject): Iterable[ObjectType] =
        super.collectInstantiatedTypes(project)
}

/**
 * This trait considers those types instantiated that can be instantiated by an application using
 * the project under analysis as a library, i.e., those types that can be instantiated because they
 * are public or inside an open package and that have a constructor that is accessible.
 *
 * @author Dominik Helm
 */
trait LibraryInstantiatedTypesFinder extends InstantiatedTypesFinder {

    override def collectInstantiatedTypes(project: SomeProject): Iterable[ObjectType] = {
        val closedPackages = project.get(ClosedPackagesKey)
        project.allClassFiles.iterator.filter { cf =>
            !cf.isInterfaceDeclaration && !cf.isAbstract &&
                (cf.isPublic /* && cf.constructors.nonEmpty*/ ||
                    !closedPackages.isClosed(cf.thisType.packageName)) &&
                    cf.constructors.exists { ctor =>
                        ctor.isPublic ||
                            !ctor.isPrivate && !closedPackages.isClosed(cf.thisType.packageName)
                    }
        }.map(_.thisType).iterator.to(Iterable) ++ super.collectInstantiatedTypes(project)
    }
}

/**
 * This trait provides an analysis that loads instantiated types from the given project
 * configuration file.
 *
 * All instantiated types must be configured under the following configuration key:
 *      **org.opalj.br.analyses.cg.InitialInstantiatedTypesKey.instantiatedTypes**
 *
 * Example:
 * {{{
 *        org.opalj.br.analyses.cg {
 *            InitialInstantiatedTypesKey {
 *                analysis = "org.opalj.br.analyses.cg.ConfigurationInstantiatedTypesFinder"
 *                instantiatedTypes = [
 *                  "java/util/List+",
 *                  "java/util/HashSet"
 *                ]
 *            }
 *        }
 *  }}}
 *
 * Please note that the first instantiated type, by adding the "+" to the class' name,
 * considers all subtypes of List as instantiated. In contrast, the second entry does not consider
 * any subtypes of HashSet (by not suffixing a plus to class), so only the single class is
 * considered to be instantiated.
 *
 * @author Michael Reif
 */
trait ConfigurationInstantiatedTypesFinder extends InstantiatedTypesFinder {

    // don't make this a val for initialization reasons
    @inline private[this] def additionalInstantiatedTypesKey: String = {
        InitialInstantiatedTypesKey.ConfigKeyPrefix+"instantiatedTypes"
    }

    override def collectInstantiatedTypes(project: SomeProject): Iterable[ObjectType] = {
        implicit val logContext = project.logContext
        var instantiatedTypes = Set.empty[ObjectType]

        if (!project.config.hasPath(additionalInstantiatedTypesKey)) {
            OPALLogger.info(
                "project configuration",
                s"configuration key $additionalInstantiatedTypesKey is missing; "+
                    "no additional types are considered instantiated configured"
            )
            return instantiatedTypes;
        }
        val configInstantiatedTypes: List[String] =
            try {
                project.config.as[List[String]](additionalInstantiatedTypesKey)
            } catch {
                case e: Throwable =>
                    OPALLogger.error(
                        "project configuration - recoverable",
                        s"configuration key $additionalInstantiatedTypesKey is invalid; "+
                            "see InstantiatedTypesFinder documentation",
                        e
                    )
                    return instantiatedTypes;
            }

        configInstantiatedTypes foreach { configuredType =>
            val considerSubtypes = configuredType.endsWith("+")
            val typeName = if (considerSubtypes) {
                configuredType.substring(0, configuredType.size - 1)
            } else {
                configuredType
            }

            val objectType = ObjectType(typeName)
            if (considerSubtypes)
                instantiatedTypes ++= project.classHierarchy.allSubtypes(objectType, true)
            else
                instantiatedTypes += objectType
        }

        super.collectInstantiatedTypes(project) ++ instantiatedTypes
    }
}

object ConfigurationInstantiatedTypesFinder
    extends ConfigurationInstantiatedTypesFinder

object ApplicationInstantiatedTypesFinder
    extends ApplicationInstantiatedTypesFinder
    with ConfigurationInstantiatedTypesFinder

object LibraryInstantiatedTypesFinder
    extends LibraryInstantiatedTypesFinder
    with ConfigurationInstantiatedTypesFinder

/**
 * The AllInstantiatedTypesFinder considers all class files' types as instantiated. It can be
 * configured to consider only project class files instead of project and library class files by
 * specifying
 * **org.opalj.br.analyses.cg.InitialInstantiatedTypesKey.AllInstantiatedTypesFinder.projectClassesOnly=true**.
 *
 * @author Dominik Helm
 */
object AllInstantiatedTypesFinder extends InstantiatedTypesFinder {
    override def collectInstantiatedTypes(project: SomeProject): Iterable[ObjectType] = {
        val projectMethodsOnlyConfigKey = InitialInstantiatedTypesKey.ConfigKeyPrefix+
            "AllInstantiatedTypesFinder.projectClassesOnly"
        val allClassFiles = if (project.config.as[Boolean](projectMethodsOnlyConfigKey))
            project.allProjectClassFiles
        else project.allClassFiles
        allClassFiles.iterator.filter { cf =>
            !cf.isInterfaceDeclaration && !cf.isAbstract
        }.map(_.thisType).to(Iterable)
    }
}
