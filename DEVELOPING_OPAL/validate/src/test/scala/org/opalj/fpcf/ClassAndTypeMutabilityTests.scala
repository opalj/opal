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
package org.opalj.fpcf

import org.opalj.fpcf.analyses.EagerClassImmutabilityAnalysis
import org.opalj.fpcf.analyses.EagerTypeImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyL1FieldMutabilityAnalysis

/**
 * Tests if the properties specified in the test project (the classes in the (sub-)package of
 * org.opalj.fpcf.fixture) and the computed ones match. The actual matching is delegated to
 * PropertyMatchers to facilitate matching arbitrary complex property specifications.
 *
 * @author Florian Kuebler
 */
class ClassAndTypeMutabilityTests extends PropertiesTest {

    describe("the field, class and type mutability analyses are executed") {
        val as = executeAnalyses(Set(
            EagerClassImmutabilityAnalysis, EagerTypeImmutabilityAnalysis
        ), Set(
            LazyL1FieldMutabilityAnalysis
        ))
        as.propertyStore.shutdown()
        validateProperties(
            as,
            classFilesWithAnnotations(as.project).map(tp ⇒ (tp._1.thisType, tp._2, tp._3)),
            Set("TypeImmutability", "ClassImmutability")
        )
    }

}
