/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

/**
 *
 * @author Florian Kuebler
 */
sealed trait InstantiatedTypesFinder {

    def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType]
}

//TODO we probably have to call super.collectInstantiatedTypes...

trait ApplicationInstantiatedTypesFinder extends InstantiatedTypesFinder {

    override def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = {
        Traversable(ObjectType.String)
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
        }.map(_.thisType).toTraversable
    }
}

trait ConfigurationInstantiatedTypesFinder extends InstantiatedTypesFinder {
    override def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = {
        // todo
        Traversable.empty
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
