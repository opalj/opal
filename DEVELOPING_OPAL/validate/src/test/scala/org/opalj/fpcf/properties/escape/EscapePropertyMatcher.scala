/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package escape

import org.opalj.ai.domain.l2.PerformInvocations
import org.opalj.ai.fpcf.properties.AIDomainFactoryKey
import org.opalj.br.AnnotationLike
import org.opalj.br.BooleanValue
import org.opalj.br.DefinedMethod
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.VirtualFormalParameter
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.tac.common.DefinitionSite

/**
 * A property matcher that checks whether an annotated allocation or parameter has the specified
 * escape property.
 *
 * @author Florian Kuebler
 */
abstract class EscapePropertyMatcher(
        val property: EscapeProperty
) extends AbstractPropertyMatcher {

    override def isRelevant(
        p:      Project[_],
        as:     Set[ObjectType],
        entity: Any,
        a:      AnnotationLike
    ): Boolean = {
        // check whether the analyses specified in the annotation are present
        val analysesElementValues = getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues map { _.asClassValue.value.asObjectType }
        val analysisRelevant = analyses.exists(as.contains)

        // check whether the PerformInvokations domain or the ArrayValuesBinding domain are required
        val requiresPerformInvokationsDomain = getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "performInvokationsDomain").asInstanceOf[BooleanValue].value
        //val requiresArrayDomain = getValue(p, a.annotationType.asObjectType, a.elementValuePairs, "arrayDomain").asInstanceOf[BooleanValue].value

        // retrieve the current method and using this the domain used for the TAC
        val m = entity match {
            case (_, VirtualFormalParameter(dm: DefinedMethod, _)) if dm.declaringClassType == dm.definedMethod.classFile.thisType =>
                dm.definedMethod
            case (_, VirtualFormalParameter(dm: DefinedMethod, _)) => return false;
            case (_, DefinitionSite(m, _))                         => m
            case _                                                 => throw new RuntimeException(s"unsuported entity $entity")
        }
        if (as.nonEmpty && m.body.isDefined) {
            val domainClass = p.get(AIDomainFactoryKey).domainClass
            val performInvocationsClass = classOf[PerformInvocations]
            val isPerformInvocationsClass = performInvocationsClass.isAssignableFrom(domainClass)

            val isPerformInvocationDomainRelevant =
                if (requiresPerformInvokationsDomain) isPerformInvocationsClass
                else !isPerformInvocationsClass

            analysisRelevant && isPerformInvocationDomainRelevant
        } else {
            analysisRelevant
        }

    }

    override def validateProperty(
        p:          Project[_],
        as:         Set[ObjectType],
        entity:     scala.Any,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists {
            case `property` => true
            case _          => false
        }) {
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

class NoEscapeMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.NoEscape)

class EscapeInCalleeMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.EscapeInCallee)

class EscapeViaParameterMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.EscapeViaParameter)

class EscapeViaReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.EscapeViaReturn)

class EscapeViaAbnormalReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.EscapeViaAbnormalReturn)

class EscapeViaParameterAndReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.EscapeViaParameterAndReturn)

class EscapeViaParameterAndAbnormalReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.EscapeViaParameterAndAbnormalReturn)

class EscapeViaNormalAndAbnormalReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.EscapeViaNormalAndAbnormalReturn)

class EscapeViaParameterAndNormalAndAbnormalReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn)

class AtMostNoEscapeMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.AtMost(org.opalj.br.fpcf.properties.NoEscape))

class AtMostEscapeInCalleeMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.AtMost(org.opalj.br.fpcf.properties.EscapeInCallee))

class AtMostEscapeViaParameterMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.AtMost(org.opalj.br.fpcf.properties.EscapeViaParameter))

class AtMostEscapeViaReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.AtMost(org.opalj.br.fpcf.properties.EscapeViaReturn))

class AtMostEscapeViaAbnormalReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.AtMost(org.opalj.br.fpcf.properties.EscapeViaAbnormalReturn))

class AtMostEscapeViaParameterAndReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.AtMost(org.opalj.br.fpcf.properties.EscapeViaParameterAndReturn))

class AtMostEscapeViaParameterAndAbnormalReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.AtMost(org.opalj.br.fpcf.properties.EscapeViaParameterAndAbnormalReturn))

class AtMostEscapeViaNormalAndAbnormalReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.AtMost(org.opalj.br.fpcf.properties.EscapeViaNormalAndAbnormalReturn))

class AtMostEscapeViaParameterAndNormalAndAbnormalReturnMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.AtMost(org.opalj.br.fpcf.properties.EscapeViaParameterAndNormalAndAbnormalReturn))

class EscapeViaStaticFieldMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.EscapeViaStaticField)

class EscapeViaHeapObjectMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.EscapeViaHeapObject)

class GlobalEscapeMatcher
    extends EscapePropertyMatcher(org.opalj.br.fpcf.properties.GlobalEscape)

