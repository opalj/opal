/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldaccess
package reflection

import scala.collection.immutable.ArraySeq

import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.br.analyses.SomeProject
import org.opalj.value.IsReferenceValue

/**
 * Used to determine whether a certain field should be considered as a target for a reflective access.
 * These accesses should be resolved by chaining matchers in [[FieldMatching.getPossibleFields]].
 *
 * @author Maximilian Rüsch
 */
trait FieldMatcher {

    def initialFields(implicit p: SomeProject): Iterator[Field]
    def priority: Int
    def contains(field: Field)(implicit p: SomeProject): Boolean

}

final class NameBasedFieldMatcher(val possibleNames: Set[String]) extends FieldMatcher {
    override def initialFields(implicit p: SomeProject): Iterator[Field] = {
        val projectIndex = p.get(ProjectIndexKey)
        possibleNames.iterator.flatMap(projectIndex.findFields)
    }
    override def priority: Int = 2
    override def contains(f: Field)(implicit p: SomeProject): Boolean = possibleNames.contains(f.name)
}

class ClassBasedFieldMatcher(
    val possibleClasses:          Set[ObjectType],
    val onlyFieldsExactlyInClass: Boolean
) extends FieldMatcher {

    // IMPROVE use a ProjectInformationKey or WeakHashMap to cache fields per class per project (for the contains check)
    private[this] def fields(implicit p: SomeProject): Set[Field] = possibleClasses.flatMap { c =>
        // IMPROVE consider inherited fields
        p.classFile(c).map(_.fields).getOrElse(ArraySeq.empty)
    }

    override def initialFields(implicit p: SomeProject): Iterator[Field] = fields.iterator
    override def priority: Int = 1
    override def contains(f: Field)(implicit p: SomeProject): Boolean = fields.contains(f)
}

/**
 * Considers all fields that have a type that is assignable to the given type. The given type thus acts as the upper
 * bound type. Usually encountered in return / cast types of field accesses.
 */
class UBTypeBasedFieldMatcher(val fieldType: FieldType) extends FieldMatcher {

    override def initialFields(implicit p: SomeProject): Iterator[Field] = p.allFields.iterator.filter(contains)
    override def priority: Int = 3
    override def contains(f: Field)(implicit p: SomeProject): Boolean =
        isTypeCompatible(fieldType, f.fieldType)(p.classHierarchy)
}

/**
 * Considers all fields that have a type to which the given type is assignable. The given type thus acts as the lower
 * bound type. Usually encountered in parameters of field accesses.
 */
class LBTypeBasedFieldMatcher(val fieldType: FieldType) extends FieldMatcher {

    override def initialFields(implicit p: SomeProject): Iterator[Field] = p.allFields.iterator.filter(contains)
    override def priority: Int = 3
    override def contains(f: Field)(implicit p: SomeProject): Boolean =
        isTypeCompatible(f.fieldType, fieldType)(p.classHierarchy)
}

/**
 * Considers all fields that either
 * - are static fields AND the receiver may be null or
 * - are instance fields AND the receiver may not be null AND the receiver is a value at least a subtype of the class
 *    declaring the field
 */
class ActualReceiverBasedFieldMatcher(val receiver: IsReferenceValue) extends FieldMatcher {

    override def initialFields(implicit p: SomeProject): Iterator[Field] =
        p.allClassFiles.iterator.flatMap { _.fields.filter(contains) }

    override def priority: Int = 3

    override def contains(f: Field)(implicit p: SomeProject): Boolean = {
        val isNull = receiver.isNull
        (isNull.isNoOrUnknown && receiver.isValueASubtypeOf(f.classFile.thisType)(p.classHierarchy).isYesOrUnknown) ||
            (isNull.isYesOrUnknown && f.isStatic)
    }

}

/**
 * Considers all fields to which the given parameter would be assignable.
 */
class ActualParameterBasedFieldMatcher(val actualParam: V) extends FieldMatcher {
    override def initialFields(implicit p: SomeProject): Iterator[Field] =
        p.allClassFiles.iterator.flatMap { _.fields.filter(contains) }

    override def priority: Int = 3

    override def contains(f: Field)(implicit p: SomeProject): Boolean =
        isTypeCompatible(f.fieldType, actualParam.value)(p.classHierarchy)

}

sealed trait PropertyBasedFieldMatcher extends FieldMatcher {

    override final def initialFields(implicit p: SomeProject): Iterator[Field] = p.allFields.iterator.filter(contains)
    override def priority: Int = 4
}

object StaticFieldMatcher extends PropertyBasedFieldMatcher {

    override def contains(f: Field)(implicit p: SomeProject): Boolean = f.isStatic
}

object NonStaticFieldMatcher extends PropertyBasedFieldMatcher {

    override def contains(f: Field)(implicit p: SomeProject): Boolean = !f.isStatic
}

object PublicFieldMatcher extends PropertyBasedFieldMatcher {

    override def contains(f: Field)(implicit p: SomeProject): Boolean = f.isPublic
}

object AllFieldsMatcher extends FieldMatcher {

    override def initialFields(implicit p: SomeProject): Iterator[Field] = p.allFields.iterator
    override def priority: Int = 5
    override def contains(f: Field)(implicit p: SomeProject): Boolean = true
}

object NoFieldsMatcher extends FieldMatcher {

    override def initialFields(implicit p: SomeProject): Iterator[Field] = Iterator.empty
    override def priority: Int = 0
    override def contains(f: Field)(implicit p: SomeProject): Boolean = false
}

object FieldMatching {

    def getPossibleFields(filters: Seq[FieldMatcher])(implicit p: SomeProject): Iterator[Field] = {
        if (filters.isEmpty) {
            Iterator.empty
        } else {
            val sortedMatchers = filters.sortBy(_.priority)
            sortedMatchers.head.initialFields.filter(f => sortedMatchers.tail.forall(_.contains(f)))
        }
    }
}
