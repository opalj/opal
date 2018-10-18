/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import net.ceedubs.ficus.Ficus._

/**
 *
 * @author Florian Kuebler
 */
sealed trait InstantiatedTypesFinder {

    def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = Traversable.empty
}

trait ApplicationInstantiatedTypesFinder extends InstantiatedTypesFinder {

    override def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = {
        Traversable(ObjectType.String) ++ super.collectInstantiatedTypes(project)
    }
}

trait LibraryInstantiatedTypesFinder extends InstantiatedTypesFinder {
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
    override def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = {
        // todo
        super.collectInstantiatedTypes(project)
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
