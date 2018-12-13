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

    def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = Traversable.empty
}

trait ApplicationInstantiatedTypesFinder
    extends InstantiatedTypesFinder
    with ConfigurationInstantiatedTypesFinder {

    override def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = {
        Traversable(ObjectType.String) ++ super.collectInstantiatedTypes(project)
    }
}

trait LibraryInstantiatedTypesFinder
    extends InstantiatedTypesFinder
        with ConfigurationInstantiatedTypesFinder {
    override def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = {
        val closedPackages = project.get(ClosedPackagesKey)
        project.allClassFiles.iterator.filter { cf ⇒
            (cf.isPublic || !closedPackages.isClosed(cf.thisType.packageName)) &&
                cf.constructors.exists { ctor ⇒
                    ctor.isPublic ||
                        !ctor.isPrivate && !closedPackages.isClosed(cf.thisType.packageName)
                }
        }.map(_.thisType).toTraversable ++ super.collectInstantiatedTypes(project)
    }
}

trait ConfigurationInstantiatedTypesFinder extends InstantiatedTypesFinder {

    // don't make this a val for initialization reasons
    @inline private[this] def additionalInstantiatedTypesKey: String = {
        InitialInstantiatedTypesKey.ConfigKeyPrefix+"instantiatedTypes"
    }

    override def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = {
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
                case e: Throwable ⇒
                    OPALLogger.error(
                        "project configuration - recoverable",
                        s"configuration key $additionalInstantiatedTypesKey is invalid; "+
                            "see InstantiatedTypesFinder documentation",
                        e
                    )
                    return instantiatedTypes;
            }

        configInstantiatedTypes foreach { configuredType ⇒
            val considerSubtypes = configuredType.endsWith("+")
            val typeName = if (considerSubtypes) {
                configuredType.substring(0, configuredType.size - 1)
            } else {
                configuredType
            }

            val objectType = ObjectType(typeName)
            if(considerSubtypes)
                instantiatedTypes += objectType
            else
                instantiatedTypes = project.classHierarchy.allSubtypes(objectType, true)
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
 * configured to consider only project class files instead of project and library class files.
 */
object AllInstantiatedTypesFinder extends InstantiatedTypesFinder {
    override def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = {
        val projectMethodsOnlyConfigKey = InitialInstantiatedTypesKey.ConfigKeyPrefix+
            "AllInstantiatedTypesFinder.projectClassesOnly"
        if (project.config.as[Boolean](projectMethodsOnlyConfigKey))
            project.allProjectClassFiles.map(_.thisType)
        else project.allClassFiles.map(_.thisType)
    }
}
