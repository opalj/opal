/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

import org.opalj.br.FieldTypes
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.ConstArray
import org.opalj.collection.immutable.RefArray
import org.opalj.value.IsPrimitiveValue
import org.opalj.value.IsReferenceValue
import org.opalj.br.BaseType
import org.opalj.br.ClassHierarchy

/**
 * Used to determine whether a certain method should be considered as a target for a reflective
 * call site. These call sites should be resolved by chaining matchers in
 * [[MethodMatching.getPossibleMethods*]].
 *
 * @author Florian Kuebler
 */
trait MethodMatcher {
    def initialMethods(implicit p: SomeProject): Iterator[Method]
    def contains(m: Method)(implicit p: SomeProject): Boolean
    def priority: Int
}

final class NameBasedMethodMatcher(val possibleNames: Set[String]) extends MethodMatcher {

    override def initialMethods(implicit p: SomeProject): Iterator[Method] = {
        val projectIndex = p.get(ProjectIndexKey)
        possibleNames.iterator.flatMap(projectIndex.findMethods)
    }

    override def contains(m: Method)(implicit p: SomeProject): Boolean = {
        possibleNames.contains(m.name)
    }

    override def priority: Int = 2
}

class ClassBasedMethodMatcher(val possibleClasses: Set[ObjectType]) extends MethodMatcher {

    // TODO use weakHashMap to cache methods per project (for the contains check)
    private[this] def methods(implicit p: SomeProject) = possibleClasses.flatMap { c ⇒
        p.instanceMethods.getOrElse(c, ConstArray.empty).map(_.method) ++
            // for static methods and constructors
            // todo what about "inherited" static methods
            p.classFile(c).map(_.methods).getOrElse(RefArray.empty)
    }

    override def initialMethods(implicit p: SomeProject): Iterator[Method] = methods.iterator

    override def contains(m: Method)(implicit p: SomeProject): Boolean = initialMethods.contains(m)

    override def priority: Int = 1
}

class ExactClassBasedMethodMatcher(val possibleClasses: Set[ObjectType]) extends MethodMatcher {
    // TODO use weakHashMap to cache methods per project (for the contains check)
    private[this] def methods(implicit p: SomeProject) = possibleClasses.flatMap { c ⇒
        p.classFile(c).map(_.methods).getOrElse(RefArray.empty)
    }

    override def initialMethods(implicit p: SomeProject): Iterator[Method] = methods.iterator

    override def contains(m: Method)(implicit p: SomeProject): Boolean = initialMethods.contains(m)

    override def priority: Int = 1
}

class DescriptorBasedMethodMatcher(
        val possibleDescriptors: Set[MethodDescriptor]
) extends MethodMatcher {

    override def initialMethods(implicit p: SomeProject): Iterator[Method] = {
        p.allMethods.iterator.filter(m ⇒ possibleDescriptors.contains(m.descriptor))
    }

    override def contains(m: Method)(implicit p: SomeProject): Boolean =
        possibleDescriptors.contains(m.descriptor)

    override def priority: Int = 3
}

class ParameterTypesBasedMethodMatcher(val parameterTypes: FieldTypes) extends MethodMatcher {

    override def initialMethods(implicit p: SomeProject): Iterator[Method] = {
        p.allMethods.iterator.filter(_.parameterTypes == parameterTypes)
    }

    override def contains(m: Method)(implicit p: SomeProject): Boolean = {
        m.parameterTypes == parameterTypes
    }

    override def priority: UShort = 3
}

class ActualParamBasedMethodMatcher(val actualParams: Seq[V]) extends MethodMatcher {

    override def initialMethods(implicit p: SomeProject): Iterator[Method] =
        p.allMethods.iterator.filter(contains)

    override def contains(m: Method)(implicit p: SomeProject): Boolean = {
        implicit val ch: ClassHierarchy = p.classHierarchy
        m.descriptor.parametersCount == actualParams.size &&
            m.descriptor.parameterTypes.zip(actualParams.map(_.value)).forall {
                // IMPROVE Make matchers nicer
                // the actual type is null and the declared type is a ref type
                case (pType, v) if pType.isReferenceType && v.isReferenceValue && v.asReferenceValue.isNull.isYes ⇒
                    // todo here we would need the declared type information
                    true
                // declared type and actual type are reference types and assignable
                case (pType, v) if pType.isReferenceType && v.isReferenceValue ⇒
                    v.asReferenceValue.isValueASubtypeOf(pType.asReferenceType).isYesOrUnknown

                // declared type and actual type are base types and the same type
                case (pType: BaseType, v: IsPrimitiveValue[_]) ⇒ v.primitiveType eq pType

                // the actual type is null and the declared type is a base type
                case (pType: BaseType, v) if v.isReferenceValue && v.asReferenceValue.isNull.isYes ⇒
                    false

                // declared type is base type, actual type might be a boxed value
                case (pType, v) if pType.isBaseType && v.isReferenceValue ⇒
                    v.asReferenceValue.isValueASubtypeOf(pType.asBaseType.WrapperType).isYesOrUnknown

                // actual type is base type, declared type might be a boxed type
                case (pType: ObjectType, v) if v.isPrimitiveValue ⇒
                    pType.isPrimitiveTypeWrapperOf(v.asPrimitiveValue.primitiveType)
                case _ ⇒
                    false
            }
    }

    override def priority: UShort = 3
}

class ActualReceiverBasedMethodMatcher(val receiver: IsReferenceValue) extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] = {
        implicit val ch: ClassHierarchy = p.classHierarchy
        p.allClassFiles.iterator.flatMap { cf ⇒
            var r = RefArray.empty[Method]
            if (receiver.isNull.isNoOrUnknown && receiver.isValueASubtypeOf(cf.thisType).isYesOrUnknown)
                r ++= cf.methods
            else if (receiver.isNull.isYesOrUnknown)
                r ++= cf.methods.filter(_.isStatic)
            r
        }
    }

    override def contains(m: Method)(implicit p: SomeProject): Boolean = {
        implicit val ch: ClassHierarchy = p.classHierarchy
        val isNull = receiver.isNull
        (isNull.isNoOrUnknown && receiver.isValueASubtypeOf(m.classFile.thisType).isYesOrUnknown) ||
            (isNull.isYesOrUnknown && m.isStatic)
    }

    override def priority: Int = 3
}

object StaticMethodMatcher extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] =
        p.allMethods.iterator.filter(contains)

    override def contains(m: Method)(implicit p: SomeProject): Boolean = m.isStatic

    override def priority: Int = 4
}

object NonStaticMethodMatcher extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] =
        p.allMethods.iterator.filter(contains)

    override def contains(m: Method)(implicit p: SomeProject): Boolean = !m.isStatic

    override def priority: Int = 4
}

object PrivateMethodMatcher extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] =
        p.allMethods.iterator.filter(contains)

    override def contains(m: Method)(implicit p: SomeProject): Boolean = m.isPrivate

    override def priority: Int = 4
}

object PublicMethodMatcher extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] =
        p.allMethods.iterator.filter(contains)

    override def contains(m: Method)(implicit p: SomeProject): Boolean = m.isPublic

    override def priority: Int = 4
}

object AllMethodsMatcher extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] = p.allMethods.iterator

    override def contains(m: Method)(implicit p: SomeProject): Boolean = true

    override def priority: Int = 5
}

object NoMethodsMatcher extends MethodMatcher {

    override def initialMethods(implicit p: SomeProject): Iterator[Method] = Iterator.empty

    override def contains(m: Method)(implicit p: SomeProject): Boolean = false

    override def priority: UShort = 0
}

object MethodMatching {

    def getPossibleMethods(
        filters: Seq[MethodMatcher]
    )(implicit p: SomeProject): Iterator[Method] = {
        if (filters.isEmpty) {
            Iterator.empty
        } else {
            val sortedMatchers = filters.sortBy(_.priority)
            sortedMatchers.head.initialMethods.filter(m ⇒ sortedMatchers.tail.forall(_.contains(m)))
        }
    }
}
