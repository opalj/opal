/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package properties
package purity

import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.br.AnnotationLike
import org.opalj.br.BooleanValue
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.br.fpcf.properties.FieldLocality
import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.properties.ReturnValueFreshness
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.properties.ClassifiedImpure
import org.opalj.br.fpcf.properties.SimpleContextsKey

/**
 * Base trait for matchers that match a method's `Purity` property.
 *
 * @author Dominik Helm
 */
sealed abstract class PurityMatcher(val property: Purity) extends AbstractPropertyMatcher {

    override def isRelevant(
        p:  SomeProject,
        as: Set[ObjectType],
        e:  Entity,
        a:  AnnotationLike
    ): Boolean = {
        val annotationType = a.annotationType.asObjectType

        val analysesElementValues =
            getValue(p, annotationType, a.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues.map(ev => ev.asClassValue.value.asObjectType)

        val eps = getValue(p, annotationType, a.elementValuePairs, "eps").asArrayValue.values.map(ev => ev.asAnnotationValue.annotation)
        val negate = getValue(p, annotationType, a.elementValuePairs, "negate").asInstanceOf[BooleanValue].value

        analyses.exists(as.contains) && eps.forall(negate ^ evaluateEP(p, as, _, negate))
    }

    def evaluateEP(
        project: SomeProject,
        as:      Set[ObjectType],
        ep:      AnnotationLike,
        negate:  Boolean
    ): Boolean = {
        val annotationType = ep.annotationType.asObjectType

        val classType = getValue(project, annotationType, ep.elementValuePairs, "cf").asClassValue.value.asObjectType

        val field =
            getValue(project, annotationType, ep.elementValuePairs, "field").asStringValue.value
        val method =
            getValue(project, annotationType, ep.elementValuePairs, "method").asStringValue.value

        val analysesElementValues =
            getValue(project, annotationType, ep.elementValuePairs, "analyses").asArrayValue.values
        val analyses = analysesElementValues.map(ev => ev.asClassValue.value.asObjectType)

        if (analyses.nonEmpty && !analyses.exists(as.contains)) {
            return !negate // Analysis specific ep requirement, but analysis was not executed
        }

        val pk = getValue(project, annotationType, ep.elementValuePairs, "pk").asStringValue.value match {
            case "Purity"               => Purity.key
            case "FieldMutability"      => FieldMutability.key
            case "ClassImmutability"    => ClassImmutability.key
            case "ReturnValueFreshness" => ReturnValueFreshness.key
            case "FieldLocality"        => FieldLocality.key
        }
        val p = getValue(project, annotationType, ep.elementValuePairs, "p").asStringValue.value

        val propertyStore = project.get(PropertyStoreKey)

        def checkProperty(eop: EOptionP[Entity, Property]): Boolean = {
            if (eop.hasUBP)
                eop.ub.toString == p
            else {
                // Here, the reason actually doesn't matter, because the fallback is always the
                // same.
                val reason = PropertyIsNotComputedByAnyAnalysis
                PropertyKey.fallbackProperty(propertyStore, reason, eop.e, eop.pk).toString == p
            }
        }

        if (field != "") {
            val cfo = project.classFile(classType)
            cfo exists { cf =>
                cf findField field exists { field =>
                    checkProperty(propertyStore(field, pk))
                }
            }
        } else if (method != "") {
            val declaredMethods = project.get(DeclaredMethodsKey)
            val descriptorIndex = method.indexOf('(')
            val methodName = method.substring(0, descriptorIndex)
            val descriptor = MethodDescriptor(method.substring(descriptorIndex))
            val cfo = project.classFile(classType)
            val simpleContexts = project.get(SimpleContextsKey)

            cfo exists { cf =>
                cf findMethod (methodName, descriptor) exists { method =>
                    checkProperty(propertyStore(simpleContexts(declaredMethods(method)), pk))
                }
            }
        } else {
            checkProperty(propertyStore(classType, pk))
        }
    }

    def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists {
            case `property` => true
            case _          => false
        }) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

sealed abstract class ContextualPurityMatcher(propertyConstructor: IntTrieSet => Purity)
    extends PurityMatcher(null) {
    override def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        val annotationType = a.annotationType.asObjectType

        val annotated =
            getValue(p, annotationType, a.elementValuePairs, "modifies").asArrayValue.values

        var modifiedParams: IntTrieSet = EmptyIntTrieSet
        annotated.foreach { param =>
            modifiedParams = modifiedParams + param.asIntValue.value
        }

        val expected = propertyConstructor(modifiedParams)

        if (!properties.exists(_ match {
            case `expected` => true
            case _          => false
        })) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }
}

/**
 * Matches a method's `Purity` property. The match is successful if the method has the property
 * [[org.opalj.br.fpcf.properties.CompileTimePure]].
 */
class CompileTimePureMatcher extends PurityMatcher(br.fpcf.properties.CompileTimePure)

/**
 * Matches a method's `Purity` property. The match is successful if the method has the property
 * [[org.opalj.br.fpcf.properties.Pure]].
 */
class PureMatcher extends PurityMatcher(br.fpcf.properties.Pure)

/**
 * Matches a method's `Purity` property. The match is successful if the method has the property
 * [[org.opalj.br.fpcf.properties.SideEffectFree]].
 */
class SideEffectFreeMatcher extends PurityMatcher(br.fpcf.properties.SideEffectFree)

/**
 * Matches a method's `Purity` property. The match is successful if the method has the property
 * [[org.opalj.br.fpcf.properties.ContextuallyPure]].
 */
class ContextuallyPureMatcher
    extends ContextualPurityMatcher(params => br.fpcf.properties.ContextuallyPure(params))

/**
 * Matches a method's `Purity` property. The match is successful if the method has the property
 * [[org.opalj.br.fpcf.properties.ContextuallySideEffectFree]].
 */
class ContextuallySideEffectFreeMatcher
    extends ContextualPurityMatcher(params => br.fpcf.properties.ContextuallySideEffectFree(params))

/**
 * Matches a method's `Purity` property. The match is successful if the method has the property
 * [[org.opalj.br.fpcf.properties.DPure]].
 */
class DomainSpecificPureMatcher extends PurityMatcher(br.fpcf.properties.DPure)

/**
 * Matches a method's `Purity` property. The match is successful if the method has the property
 * [[org.opalj.br.fpcf.properties.DSideEffectFree]].
 */
class DomainSpecificSideEffectFreeMatcher extends PurityMatcher(br.fpcf.properties.DSideEffectFree)

/**
 * Matches a method's `Purity` property. The match is successful if the method has the property
 * [[org.opalj.br.fpcf.properties.DContextuallyPure]].
 */
class DomainSpecificContextuallyPureMatcher
    extends ContextualPurityMatcher(params => br.fpcf.properties.DContextuallyPure(params))

/**
 * Matches a method's `Purity` property. The match is successful if the method has the property
 * [[org.opalj.br.fpcf.properties.DContextuallySideEffectFree]].
 */
class DomainSpecificContextuallySideEffectFreeMatcher
    extends ContextualPurityMatcher(params => br.fpcf.properties.DContextuallySideEffectFree(params))

/**
 * Matches a method's `Purity` property. The match is successful if the property is an instance of
 * [[org.opalj.br.fpcf.properties.ClassifiedImpure]].
 */
class ImpureMatcher extends PurityMatcher(null) {

    override def validateProperty(
        p:          SomeProject,
        as:         Set[ObjectType],
        entity:     Entity,
        a:          AnnotationLike,
        properties: Iterable[Property]
    ): Option[String] = {
        if (!properties.exists {
            case _: ClassifiedImpure => true
            case _                   => false
        }) {
            // ... when we reach this point the expected property was not found.
            Some(a.elementValuePairs.head.value.asStringValue.value)
        } else {
            None
        }
    }

}
