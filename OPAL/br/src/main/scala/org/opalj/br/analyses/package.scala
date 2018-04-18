/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package br

import scala.collection.Map

import org.opalj.collection.QualifiedCollection
import org.opalj.collection.immutable.ConstArray

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

    type DeclaredMethods = Map[ObjectType, QualifiedCollection[Set[Method]]]

    type ProjectInformationKeys = Seq[ProjectInformationKey[_ <: AnyRef, _ <: AnyRef]]

    type StringConstantsInformation = Map[String, ConstArray[PCInMethod]]

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
