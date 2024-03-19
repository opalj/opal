/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

import scala.collection.immutable.ArraySeq

import org.opalj.br.ClassHierarchy
import org.opalj.br.FieldTypes
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.br.analyses.SomeProject
import org.opalj.value.IsReferenceValue

/**
 * Used to determine whether a certain method should be considered as a target for a reflective
 * call site. These call sites should be resolved by chaining matchers in
 * [[MethodMatching.getPossibleMethods*]].
 *
 * @author Florian Kuebler
 */
trait MethodMatcher {
    def initialMethods(implicit p: SomeProject): Iterator[Method]
    def priority: Int
    def contains(m: Method)(implicit p: SomeProject): Boolean
}

final class NameBasedMethodMatcher(val possibleNames: Set[String]) extends MethodMatcher {

    override def initialMethods(implicit p: SomeProject): Iterator[Method] = {
        val projectIndex = p.get(ProjectIndexKey)
        possibleNames.iterator.flatMap(projectIndex.findMethods)
    }

    override def priority: Int = 2

    override def contains(m: Method)(implicit p: SomeProject): Boolean = {
        possibleNames.contains(m.name)
    }
}

class ClassBasedMethodMatcher(
    val possibleClasses:           Set[ObjectType],
    val onlyMethodsExactlyInClass: Boolean
) extends MethodMatcher {

    // TODO use a ProjectInformationKey or WeakHashMap to cache methods per project
    // (for the contains check)
    private[this] def methods(implicit p: SomeProject): Set[Method] = possibleClasses.flatMap { c =>
        // todo what about "inherited" static methods?
        val methodsInClassFile = p.classFile(c).map(_.methods).getOrElse(ArraySeq.empty)
        if (onlyMethodsExactlyInClass)
            methodsInClassFile
        else
            methodsInClassFile ++ p.instanceMethods.getOrElse(c, ArraySeq.empty).map(_.method)

    }

    override def initialMethods(implicit p: SomeProject): Iterator[Method] = methods.iterator

    override def priority: Int = 1

    override def contains(m: Method)(implicit p: SomeProject): Boolean = methods.contains(m)
}

class DescriptorBasedMethodMatcher(
    val possibleDescriptors: Set[MethodDescriptor]
) extends MethodMatcher {

    override def initialMethods(implicit p: SomeProject): Iterator[Method] = {
        p.allMethods.iterator.filter(m => possibleDescriptors.contains(m.descriptor))
    }

    override def priority: Int = 3

    override def contains(m: Method)(implicit p: SomeProject): Boolean =
        possibleDescriptors.contains(m.descriptor)
}

class ParameterTypesBasedMethodMatcher(val parameterTypes: FieldTypes) extends MethodMatcher {

    override def initialMethods(implicit p: SomeProject): Iterator[Method] = {
        p.allMethods.iterator.filter(_.parameterTypes == parameterTypes)
    }

    override def priority: UShort = 3

    override def contains(m: Method)(implicit p: SomeProject): Boolean = {
        m.parameterTypes == parameterTypes
    }
}

class ActualParameterBasedMethodMatcher(val actualParams: Seq[V]) extends MethodMatcher {

    override def initialMethods(implicit p: SomeProject): Iterator[Method] =
        p.allMethods.iterator.filter(contains)

    override def priority: UShort = 3

    override def contains(m: Method)(implicit p: SomeProject): Boolean = {
        implicit val ch: ClassHierarchy = p.classHierarchy
        // IMPROVE: actualParams.size is actual O(n) in some cases, making it an IndexedSeq would
        // however require to change it in `Call` in TACAI.
        m.descriptor.parametersCount == actualParams.size &&
            // IMPROVE: m.descriptor.parameterTypes.iterator.zip...
            // therefor, we need to actualParams.map(...) as Iterator
            m.descriptor.parameterTypes.zip(actualParams.map(_.value)).forall {
                case (formal, actual) => isTypeCompatible(formal, actual)
            }
    }
}

class ActualReceiverBasedMethodMatcher(val receiver: IsReferenceValue) extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] = {
        implicit val ch: ClassHierarchy = p.classHierarchy
        p.allClassFiles.iterator.flatMap { cf =>
            var r = ArraySeq.empty[Method]
            if (receiver.isNull.isNoOrUnknown && receiver.isValueASubtypeOf(cf.thisType).isYesOrUnknown)
                r ++= cf.methods
            else if (receiver.isNull.isYesOrUnknown)
                r ++= cf.methods.filter(_.isStatic)
            r
        }
    }

    override def priority: Int = 3

    override def contains(m: Method)(implicit p: SomeProject): Boolean = {
        implicit val ch: ClassHierarchy = p.classHierarchy
        val isNull = receiver.isNull
        (isNull.isNoOrUnknown && receiver.isValueASubtypeOf(m.classFile.thisType).isYesOrUnknown) ||
            (isNull.isYesOrUnknown && m.isStatic)
    }
}

object StaticMethodMatcher extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] =
        p.allMethods.iterator.filter(contains)

    override def priority: Int = 4

    override def contains(m: Method)(implicit p: SomeProject): Boolean = m.isStatic
}

object NonStaticMethodMatcher extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] =
        p.allMethods.iterator.filter(contains)

    override def priority: Int = 4

    override def contains(m: Method)(implicit p: SomeProject): Boolean = !m.isStatic
}

object PrivateMethodMatcher extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] =
        p.allMethods.iterator.filter(contains)

    override def priority: Int = 4

    override def contains(m: Method)(implicit p: SomeProject): Boolean = m.isPrivate
}

object PublicMethodMatcher extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] =
        p.allMethods.iterator.filter(contains)

    override def priority: Int = 4

    override def contains(m: Method)(implicit p: SomeProject): Boolean = m.isPublic
}

object AllMethodsMatcher extends MethodMatcher {
    override def initialMethods(implicit p: SomeProject): Iterator[Method] = p.allMethods.iterator

    override def priority: Int = 5

    override def contains(m: Method)(implicit p: SomeProject): Boolean = true
}

object NoMethodsMatcher extends MethodMatcher {

    override def initialMethods(implicit p: SomeProject): Iterator[Method] = Iterator.empty

    override def priority: UShort = 0

    override def contains(m: Method)(implicit p: SomeProject): Boolean = false
}

object MethodMatching {

    def getPossibleMethods(
        filters: Seq[MethodMatcher]
    )(implicit p: SomeProject): Iterator[Method] = {
        if (filters.isEmpty) {
            Iterator.empty
        } else {
            val sortedMatchers = filters.sortBy(_.priority)
            sortedMatchers.head.initialMethods.filter(m => sortedMatchers.tail.forall(_.contains(m)))
        }
    }
}
