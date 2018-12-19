/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
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
import org.opalj.value.IsReferenceValue

/**
 * @author Florian Kuebler
 */
trait MethodMatcher {
    def initialMethods: Iterator[Method]
    def contains(m: Method): Boolean
    def priority: Int
}

class NameBasedMethodMatcher(possibleNames: Set[String], project: SomeProject) extends MethodMatcher {
    private val projectIndex = project.get(ProjectIndexKey)
    override def initialMethods: Iterator[Method] = {
        possibleNames.iterator.flatMap(projectIndex.findMethods)
    }

    override def contains(m: Method): Boolean = possibleNames.contains(m.name)

    override def priority: Int = 2
}

// todo we may need a exactly in this class filter (for getDeclaredMethod)
class ClassBasedMethodMatcher(possibleClasses: Set[ObjectType], project: SomeProject) extends MethodMatcher {

    private[this] def methods = possibleClasses.flatMap { c ⇒
        project.instanceMethods.getOrElse(c, ConstArray.empty).map(_.method) ++
            // for static methods and constructors
            // todo what about "inherited" static methods
            project.classFile(c).map(_.methods).getOrElse(RefArray.empty)
    }

    override def initialMethods: Iterator[Method] = methods.iterator

    override def contains(m: Method): Boolean = initialMethods.contains(m)

    override def priority: Int = 1
}

class DescriptorBasedMethodMatcher(
        possibleDescriptors: Set[MethodDescriptor], project: SomeProject
) extends MethodMatcher {
    override def initialMethods: Iterator[Method] = {
        project.allMethods.iterator.filter(m ⇒ possibleDescriptors.contains(m.descriptor))
    }

    override def contains(m: Method): Boolean = possibleDescriptors.contains(m.descriptor)

    override def priority: Int = 3
}

class ParameterTypesBasedMethodMatcher(
        parameterTypes: FieldTypes, project: SomeProject
) extends MethodMatcher {
    override def initialMethods: Iterator[Method] = {
        project.allMethods.iterator.filter(_.parameterTypes == parameterTypes)
    }

    override def contains(m: Method): Boolean = {
        m.parameterTypes == parameterTypes
    }

    override def priority: UShort = 3
}

class ActualParamBasedMethodMatcher(
        actualParams: Seq[V], project: SomeProject
) extends MethodMatcher {
    override def initialMethods: Iterator[Method] = project.allMethods.iterator.filter(contains)

    override def contains(m: Method): Boolean = {
        m.descriptor.parametersCount == actualParams.size &&
            m.descriptor.parameterTypes.zip(actualParams.map(_.value)).forall {
                // the actual type is null and the declared type is a ref type
                case (pType, v) if pType.isReferenceType && v.isReferenceValue && v.asReferenceValue.isNull.isYes ⇒
                    // todo here we would need the declared type information
                    true
                // declared type and actual type are reference types and assignable
                case (pType, v) if pType.isReferenceType && v.isReferenceValue ⇒
                    v.asReferenceValue.isValueASubtypeOf(pType.asReferenceType).isYesOrUnknown

                // declared type and actual type are base types and the same type
                case (pType, v) if pType.isBaseType && v.isPrimitiveValue ⇒
                    v.asPrimitiveValue.primitiveType eq pType

                // the actual type is null and the declared type is a base type
                case (pType, v) if pType.isBaseType && v.isReferenceValue && v.asReferenceValue.isNull.isYes ⇒
                    false

                // declared type is base type, actual type might be a boxed value
                case (pType, v) if pType.isBaseType && v.isReferenceValue ⇒
                    v.asReferenceValue.isValueASubtypeOf(pType.asBaseType.WrapperType).isYesOrUnknown

                // actual type is base type, declared type might be a boxed type
                case (pType, v) if pType.isObjectType && v.isPrimitiveValue ⇒
                    pType.asObjectType.isPrimitiveTypeWrapperOf(
                        v.asPrimitiveValue.primitiveType
                    )
                case _ ⇒
                    false
            }
    }

    override def priority: UShort = 3
}

// todo rename as this is only for the first argument of Method.invoke
class ActualReceiverBasedMethodMatcher(
        receiver: IsReferenceValue, project: SomeProject
) extends MethodMatcher {
    override def initialMethods: Iterator[Method] = {
        project.allClassFiles.iterator.flatMap { cf ⇒
            var r = RefArray.empty[Method]
            if (receiver.isNull.isNoOrUnknown && receiver.isValueASubtypeOf(cf.thisType).isYesOrUnknown)
                r ++= cf.methods
            else if (receiver.isNull.isYesOrUnknown)
                r ++= cf.methods.filter(_.isStatic)
            r
        }
    }

    override def contains(m: Method): Boolean =
        (receiver.isNull.isNoOrUnknown && receiver.isValueASubtypeOf(m.classFile.thisType).isYesOrUnknown) ||
            (receiver.isNull.isYesOrUnknown && m.isStatic)

    override def priority: Int = 3
}

class StaticMethodMatcher(project: SomeProject) extends MethodMatcher {
    override def initialMethods: Iterator[Method] = project.allMethods.iterator.filter(contains)

    override def contains(m: Method): Boolean = m.isStatic

    override def priority: Int = 4
}

class NonStaticMethodMatcher(project: SomeProject) extends MethodMatcher {
    override def initialMethods: Iterator[Method] = project.allMethods.iterator.filter(contains)

    override def contains(m: Method): Boolean = !m.isStatic

    override def priority: Int = 4
}

class PrivateMethodMatcher(project: SomeProject) extends MethodMatcher {
    override def initialMethods: Iterator[Method] = project.allMethods.iterator.filter(contains)

    override def contains(m: Method): Boolean = m.isPrivate

    override def priority: Int = 4
}

class PublicMethodMatcher(project: SomeProject) extends MethodMatcher {
    override def initialMethods: Iterator[Method] = project.allMethods.iterator.filter(contains)

    override def contains(m: Method): Boolean = m.isPublic

    override def priority: Int = 4
}

class AllMethodsMatcher(project: SomeProject) extends MethodMatcher {
    override def initialMethods: Iterator[Method] = project.allMethods.iterator

    override def contains(m: Method): Boolean = true

    override def priority: Int = 5
}

object NoMethodsMatcher extends MethodMatcher {
    override def initialMethods: Iterator[Method] = Iterator.empty

    override def contains(m: Method): Boolean = false

    override def priority: UShort = 0
}

object MethodMatching {
    def getPossibleMethods(filters: Seq[MethodMatcher]): Iterator[Method] = {
        if (filters.isEmpty) {
            Iterator.empty
        } else {
            val sortedMatchers = filters.sortBy(_.priority)
            sortedMatchers.head.initialMethods.filter(m ⇒ filters.forall(_.contains(m)))
        }
    }
}
