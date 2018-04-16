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
package properties
package thrown_exceptions

import org.opalj.br.AnnotationLike
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.fpcf.Entity
import org.opalj.fpcf.Property

/**
 * Matches a methods's `ThrownExceptions` property.
 *
 * @author Andreas Muttscheller
 */
class DoesNotThrowExceptionMatcher extends AbstractPropertyMatcher {

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Traversable[Property]
    ): Option[String] = {
        val annotationType = a.annotationType.asObjectType
        val analysesElementValues =
            getValue(p, annotationType, a.elementValuePairs, "requires").asArrayValue.values
        val requiredAnalysis = analysesElementValues.map(ev ⇒ ev.asClassValue.value.asObjectType)

        // Succeed either if the required analysis did NOT run, or the property is correct
        val isPropertyValid = !requiredAnalysis.exists(as.contains) ||
            properties.forall { p ⇒
                p.key != ThrownExceptions.Key || // If we got another key, ignore
                    (p.isInstanceOf[ThrownExceptions] && p.asInstanceOf[ThrownExceptions].throwsNoExceptions)
            }
        if (isPropertyValid)
            None
        else {
            Some(a.elementValuePairs.head.value.toString)
        }
    }

}
