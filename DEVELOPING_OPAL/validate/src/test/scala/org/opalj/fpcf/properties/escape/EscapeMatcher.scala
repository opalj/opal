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
package escape
import org.opalj.br.analyses.Project
import org.opalj.br.ObjectType
import org.opalj.br.AnnotationLike

abstract class EscapeMatcher(val property: EscapeProperty) extends AbstractPropertyMatcher {
    override def isRelevant(p: Project[_], as: Set[ObjectType], entity: Any, a: AnnotationLike): Boolean = {
        val analysesElementValues = getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues map { _.asClassValue.value.asObjectType }
        //val domainsElementValues = getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "domains").asArrayValue.values
        //val domains = domainsElementValues map { _.asClassValue.value.asObjectType }
        //val provider = p.get(SimpleAIKey)
        analyses.exists(as.contains)
    }

    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: List[Property]
    ): Option[String] = {
        if (!properties.exists {
            case `property` ⇒ true
            case _          ⇒ false
        }) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

class NoEscapeMatcher extends EscapeMatcher(org.opalj.fpcf.properties.NoEscape)
class EscapeInCalleeMatcher extends EscapeMatcher(org.opalj.fpcf.properties.EscapeInCallee)
class EscapeViaParameterMatcher extends EscapeMatcher(org.opalj.fpcf.properties.EscapeViaParameter)
class EscapeViaReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.EscapeViaReturn)
class EscapeViaAbnormalReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.EscapeViaAbnormalReturn)
class EscapeViaParameterAndReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.EscapeViaParameterAndReturn)
class EscapeViaParameterAndAbnormalReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.EscapeViaParameterAndAbnormalReturn)
class EscapeViaNormalAndAbnormalReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.EscapeViaNormalAndAbnormalReturn)
class EscapeViaParameterAndNormalAndAbnormalReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn)
class MaybeNoEscapeMatcher extends EscapeMatcher(org.opalj.fpcf.properties.MaybeNoEscape)
class MaybeEscapeInCalleeMatcher extends EscapeMatcher(org.opalj.fpcf.properties.MaybeEscapeInCallee)
class MaybeEscapeViaParameterMatcher extends EscapeMatcher(org.opalj.fpcf.properties.MaybeEscapeViaParameter)
class MaybeEscapeViaReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.MaybeEscapeViaReturn)
class MaybeEscapeViaAbnormalReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.MaybeEscapeViaAbnormalReturn)
class MaybeEscapeViaParameterAndReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.MaybeEscapeViaParameterAndReturn)
class MaybeEscapeViaParameterAndAbnormalReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.MaybeEscapeViaParameterAndAbnormalReturn)
class MaybeEscapeViaNormalAndAbnormalReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.MaybeEscapeViaNormalAndAbnormalReturn)
class MaybeEscapeViaParameterAndNormalAndAbnormalReturnMatcher extends EscapeMatcher(org.opalj.fpcf.properties.MaybeEscapeViaParameterAndNormalAndAbnormalReturn)
class EscapeViaStaticFieldMatcher extends EscapeMatcher(org.opalj.fpcf.properties.EscapeViaStaticField)
class EscapeViaHeapObjectMatcher extends EscapeMatcher(org.opalj.fpcf.properties.EscapeViaHeapObject)
class GlobalEscapeMatcher extends EscapeMatcher(org.opalj.fpcf.properties.GlobalEscape)

