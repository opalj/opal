/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * The ''key'' object to get an index of the source elements of a project.
 *
 * @example
 *      To get the index use the [[Project]]'s `get` method and pass in
 *      `this` object.
 *
 * @author Michael Eichberg
 */
object ProjectIndexKey extends ProjectInformationKey[ProjectIndex, Nothing] {

    /**
     * The [[ProjectIndex]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Computes the [[ProjectIndex]] for the given project.
     */
    override def compute(project: SomeProject): ProjectIndex = ProjectIndex(project)

}

