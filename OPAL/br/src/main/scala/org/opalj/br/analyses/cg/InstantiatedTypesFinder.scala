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

trait ApplicationInstantiatedTypesFinder extends InstantiatedTypesFinder {

    override def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = {
        Traversable(ObjectType.String)
    }
}

trait LibraryInstantiatedTypesFinder extends InstantiatedTypesFinder {
    override def collectInstantiatedTypes(project: SomeProject): Traversable[ObjectType] = {
        // todo more precise!
        project.allClassFiles.map(_.thisType)
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
