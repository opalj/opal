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

/**
 * A test that looks if the classifications of the SimpleImmutableAnalysis
 * matches the mutability annotiations.
 *
 * @author Andre Pacak
 */
@RunWith(classOf[JUnitRunner])
class ImmutabilityAnalysisTest
        extends FlatSpec
        with Matchers {

    import ImmutabilityAnalysisTest._

    val immutableAnnotation =
        ObjectType("immutability/annotations/Immutable")
    val mutableAnnotation =
        ObjectType("immutability/annotations/Mutable")

    val result = ImmutabilityAnalysis.doAnalyze(project, () ⇒ false)

    behavior of "bytecode representations immutability check"

    for {
        (objectType, classification) ← result
        classFile ← project.classFile(objectType)
        if !classFile.isInnerClass
    } yield {
        //we assume that a class has only one annotation concerning mutability
        //and has not other annotations attached to it
        val m = classFile.annotations.filter {
            annotation ⇒
                annotation.annotationType == immutableAnnotation ||
                    annotation.annotationType == mutableAnnotation
        }
        if (m.nonEmpty) {
            val hasMutableAnnotation = m.head.annotationType == mutableAnnotation
            val mutability =
                if (hasMutableAnnotation)
                    "not be marked as immmutable"
                else
                    "be marked as immmutable"
            val isNotImmutable = classification != Immutability.Immutable
            //check that no objectType that is mutable gets classfied as immutable
            if (hasMutableAnnotation) {
                classFile.thisType.simpleName should mutability in {

                    isNotImmutable should be(hasMutableAnnotation)

                }
            }
        }
    }
}

private object ImmutabilityAnalysisTest {
    val resources = locateTestResources("classfiles/immutability.jar", "ai")
    val project = Project(resources)

}