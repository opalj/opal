/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package ai
package analyses

import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.Matchers
import org.opalj.bi.TestSupport.locateTestResources
import org.opalj.br.analyses.Project
import org.opalj.br.ObjectType
import org.scalatest.junit.JUnitRunner

import org.opalj.ai.analyses.MutabilityRating._

/**
 * Tests that the rating of the `ImmutabilityAnalysis` is always a sound approximation.
 *
 * @author Andre Pacak
 */
@RunWith(classOf[JUnitRunner])
class ImmutabilityAnalysisTest extends FlatSpec with Matchers {

    import ImmutabilityAnalysisTest._

    final val ImmutableAnnotation =
        ObjectType("immutability/annotations/Immutable")
    final val ConditionallyImmutableAnnotation =
        ObjectType("immutability/annotations/ConditionallyImmutable")
    final val MutableAnnotation =
        ObjectType("immutability/annotations/Mutable")

    behavior of "the ImmutabiliyAnalysis"

    for {
        (objectType, mutabilityRating) ← ImmutabilityAnalysis.doAnalyze(project)
        classFile ← project.classFile(objectType)
        // This test assumes that a class has at most one that specifies its mutability.
        expectedMutabilityRating = classFile.annotations.map(_.annotationType).collectFirst[MutabilityRating] {
            case ImmutableAnnotation              ⇒ Immutable
            case ConditionallyImmutableAnnotation ⇒ ConditionallyImmutable
            case MutableAnnotation                ⇒ Mutable
        }
        // the jar also contains some "helper" classes without a mutability rating
        if expectedMutabilityRating.isDefined
    } yield {
        it should s"rate the class ${objectType.toJava} as ${expectedMutabilityRating.get} (or less)" in {
            if (mutabilityRating.id > expectedMutabilityRating.get.id)
                fail(s"the result of the immutability analysis is $mutabilityRating")
            else
                info(s"actual classification: ${mutabilityRating}")
        }
    }
}

private object ImmutabilityAnalysisTest {

    val resources = locateTestResources("classfiles/immutability.jar", "br")

    val project = Project(resources)

}