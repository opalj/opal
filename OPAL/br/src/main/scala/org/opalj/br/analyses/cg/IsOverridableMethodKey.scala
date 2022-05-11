/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

/**
 * The ''key'' object to get a function that determines whether a method can be overridden by a not
 * yet existing type. A method can be overridden if it's declaring type ___dt___is extensible by an
 * (unknown) type ___ut___ (e.g., when the analysis assumes an open world) and if the method is not
 * overridden by another subtype ___s___ such that ___ut <: s <: st___ and if the method can be
 * overridden according to the JVM's semantics.
 *
 * @author Michael Reif
 */
object IsOverridableMethodKey extends ProjectInformationKey[Method => Answer, Nothing] {

    /**
     * The [[IsOverridableMethodKey]] has the [[TypeExtensibilityKey]] as prerequisite.
     *
     * @return Seq(TypeExtensibilityKey).
     */
    override def requirements(project: SomeProject): ProjectInformationKeys = Seq(TypeExtensibilityKey)

    override def compute(project: SomeProject): Method => Answer = {
        new IsOverridableMethodAnalysis(
            project,
            project.get(ClassExtensibilityKey),
            project.get(TypeExtensibilityKey)
        )
    }
}
