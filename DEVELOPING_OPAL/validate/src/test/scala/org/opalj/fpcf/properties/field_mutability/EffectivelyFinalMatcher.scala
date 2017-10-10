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
package fpcf
package properties
package field_mutability

import org.opalj.br.Annotation
import org.opalj.br.ObjectType
import org.opalj.br.ElementValuePairs
import org.opalj.br.ElementValue
import org.opalj.br.ElementValuePair
import org.opalj.br.analyses.SomeProject

/**
 * Matches a field's `FieldMutability` property. The match is successful if the field either
 * does not have a corresponding property (in which case the fallback property will be
 * `NonFinalField`) or if the property is an instance of `NonFinalField`.
 *
 * @author Michael Eichberg
 */
class EffectivelyFinalMatcher extends PropertyMatcher {

    def getValue(
        p:              SomeProject,
        annotationType: ObjectType,
        evps:           ElementValuePairs,
        elementName:    String
    ): ElementValue = {
        evps.collectFirst {
            case ElementValuePair(`elementName`, value) ⇒ value
        }.orElse {
            // get default value ...
            p.classFile(annotationType).get.findMethod(elementName).head.annotationDefault
        }.get
    }

    final val PropertyReasonID = 0
    final val AnalysesValueId = 1 // the index of the "analyses" key of the effectively final analysis

    def hasProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          Annotation,
        properties: List[Property]
    ): Option[String] = {
        val analysesElementValues =
            getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues.map(ev ⇒ ev.asClassValue.value.asObjectType)
        if (analyses.exists(as.contains) && !properties.contains(EffectivelyFinalField)) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs(PropertyReasonID).value.asStringValue.value)
        } else {
            None
        }
    }

}
