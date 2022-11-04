/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

/**
 * '''Key'' to get the function that determines whether a type (i.e., we abstract over a class/
 * interface and all its subtypes) is extensible or not.
 * A type is extensible if a developer could define a sub(*)type that is not part of the given
 * application/library.
 *
 * @author Michael Eichberg
 * @author Michael Reif
 */
object TypeExtensibilityKey extends ProjectInformationKey[ObjectType => Answer, Nothing] {

    /**
     * The [[TypeExtensibilityKey]] has the [[ClassExtensibilityKey]] as prerequisite.
     *
     * @return Seq(ClassExtensibilityKey).
     */
    override def requirements(project: SomeProject) = Seq(ClassExtensibilityKey)

    override def compute(project: SomeProject): ObjectType => Answer = new TypeExtensibilityAnalysis(project)
}
