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
import org.opalj.ai.DefinitionSite
import org.opalj.ai.common.SimpleAIKey
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.ai.domain.l2.PerformInvocations
import org.opalj.br.analyses.Project
import org.opalj.br.ObjectType
import org.opalj.br.AnnotationLike
import org.opalj.br.BooleanValue

/**
 * A property matcher that checks whether an annotated allocation or parameter has the specified
 * escape property.
 *
 * @author Florian Kuebler
 */
abstract class EscapePropertyMatcher(val property: EscapeProperty) extends AbstractPropertyMatcher {
    override def isRelevant(p: Project[_], as: Set[ObjectType], entity: Any, a: AnnotationLike): Boolean = {
        // check whether the analyses specified in the annotation are present
        val analysesElementValues = getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues map { _.asClassValue.value.asObjectType }
        val analysisRelevant = analyses.exists(as.contains)

        // check whether the PerformInvokations domain or the ArrayValuesBinding domain are required
        val requiresPerformInvokationsDomain = getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "performInvokationsDomain").asInstanceOf[BooleanValue].value
        //val requiresArrayDomain = getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "arrayDomain").asInstanceOf[BooleanValue].value

        // retrieve the current method and using this the domain used for the TAC
        val m = entity match {
            case VirtualFormalParameter(DefinedMethod(dc, m), _) if dc == m.classFile.thisType ⇒ m
            case VirtualFormalParameter(DefinedMethod(_, _), _) ⇒ return false
            case DefinitionSite(m, _, _) ⇒ m
            case _ ⇒ throw new RuntimeException(s"unsuported entity $entity")
        }
        if (as.nonEmpty && m.body.isDefined) {
            val domain = p.get(SimpleAIKey)(m).domain

            val performInvokationDomainRelevant =
                if (requiresPerformInvokationsDomain) domain.isInstanceOf[PerformInvocations]
                else !domain.isInstanceOf[PerformInvocations]

            analysisRelevant && performInvokationDomainRelevant
        } else {
            analysisRelevant
        }

    }

    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Traversable[Property]
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

class NoEscapeMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.NoEscape)
class EscapeInCalleeMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.EscapeInCallee)
class EscapeViaParameterMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.EscapeViaParameter)
class EscapeViaReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.EscapeViaReturn)
class EscapeViaAbnormalReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.EscapeViaAbnormalReturn)
class EscapeViaParameterAndReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.EscapeViaParameterAndReturn)
class EscapeViaParameterAndAbnormalReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.EscapeViaParameterAndAbnormalReturn)
class EscapeViaNormalAndAbnormalReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.EscapeViaNormalAndAbnormalReturn)
class EscapeViaParameterAndNormalAndAbnormalReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn)
class AtMostNoEscapeMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.AtMost(org.opalj.fpcf.properties.NoEscape))
class AtMostEscapeInCalleeMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.AtMost(org.opalj.fpcf.properties.EscapeInCallee))
class AtMostEscapeViaParameterMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.AtMost(org.opalj.fpcf.properties.EscapeViaParameter))
class AtMostEscapeViaReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.AtMost(org.opalj.fpcf.properties.EscapeViaReturn))
class AtMostEscapeViaAbnormalReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.AtMost(org.opalj.fpcf.properties.EscapeViaAbnormalReturn))
class AtMostEscapeViaParameterAndReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.AtMost(org.opalj.fpcf.properties.EscapeViaParameterAndReturn))
class AtMostEscapeViaParameterAndAbnormalReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.AtMost(org.opalj.fpcf.properties.EscapeViaParameterAndAbnormalReturn))
class AtMostEscapeViaNormalAndAbnormalReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.AtMost(org.opalj.fpcf.properties.EscapeViaNormalAndAbnormalReturn))
class AtMostEscapeViaParameterAndNormalAndAbnormalReturnMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.AtMost(org.opalj.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn))
class EscapeViaStaticFieldMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.EscapeViaStaticField)
class EscapeViaHeapObjectMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.EscapeViaHeapObject)
class GlobalEscapeMatcher extends EscapePropertyMatcher(org.opalj.fpcf.properties.GlobalEscape)

