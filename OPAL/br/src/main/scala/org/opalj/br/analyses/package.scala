/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import org.opalj.si.{MetaProject, ProjectInformationKey}

import scala.collection.Map
import scala.collection.immutable.ArraySeq

/**
 * Defines commonly useful type aliases.
 *
 * @author Michael Eichberg
 */
package object analyses {

    /**
     * Type alias for Project's with an arbitrary sources.
     */
    type SomeProject = Project[_]

    type ProgressEvent = ProgressEvents.Value

    //trait JavaProjectInformationKeys extends Seq[JavaProjectInformationKey[_ <: AnyRef, _ <: AnyRef]]
    type JavaProjectInformationKeys = Seq[ProjectInformationKey[_ <: MetaProject, _ <: AnyRef, _ <: AnyRef]]

    type StringConstantsInformation = Map[String, ArraySeq[PCInMethod]]

    /**
     * An analysis that may produce a result.
     */
    type SingleOptionalResultAnalysis[Source, +AnalysisResult] = Analysis[Source, Option[AnalysisResult]]

    /**
     * An analysis that may produce multiple results. E.g., an analysis that looks for
     * instances of bug patterns.
     */
    type MultipleResultsAnalysis[Source, +AnalysisResult] = Analysis[Source, Iterable[AnalysisResult]]

    implicit object MethodDeclarationContextOrdering extends Ordering[MethodDeclarationContext] {
        def compare(x: MethodDeclarationContext, y: MethodDeclarationContext): Int = x compare y
    }

}
