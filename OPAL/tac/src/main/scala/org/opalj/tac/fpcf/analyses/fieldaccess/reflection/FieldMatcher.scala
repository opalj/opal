/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldaccess
package reflection

import org.opalj.br.BaseType
import org.opalj.br.CTIntType
import org.opalj.br.ClassHierarchy
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Field
import org.opalj.br.FieldType
import org.opalj.br.IntLikeType
import org.opalj.br.ReferenceType
import org.opalj.tac.fpcf.analyses.cg.V
import org.opalj.value.IsIntegerLikeValue
import org.opalj.value.IsNullValue
import org.opalj.value.IsPrimitiveValue
import org.opalj.value.IsReferenceValue

import scala.collection.immutable.ArraySeq

/**
 * Used to determine whether a certain field should be considered as a target for a reflective access.
 * These accesses should be resolved by chaining matchers in [[FieldMatching.getPossibleFields]].
 *
 * @author Maximilian Rüsch
 */
trait FieldMatcher {

    def initialFields(implicit p: SomeProject): Iterator[Field]
    def contains(field: Field)(implicit p: SomeProject): Boolean
    def priority: Int
}

final class NameBasedFieldMatcher(val possibleNames: Set[String]) extends FieldMatcher {

    override def initialFields(implicit p: SomeProject): Iterator[Field] = {
        possibleNames.iterator.flatMap(p.get(ProjectIndexKey).findFields)
    }
    override def contains(f: Field)(implicit p: SomeProject): Boolean = possibleNames.contains(f.name)
    override def priority: Int = 2
}

class ClassBasedFieldMatcher(
        val possibleClasses:          Set[ObjectType],
        val onlyFieldsExactlyInClass: Boolean
) extends FieldMatcher {

    // TODO use a ProjectInformationKey or WeakHashMap to cache fields per project
    // (for the contains check)
    private[this] def fields(implicit p: SomeProject): Set[Field] = possibleClasses.flatMap { c =>
        // todo what about "inherited" fields?
        p.classFile(c).map(_.fields).getOrElse(ArraySeq.empty)
    }

    override def initialFields(implicit p: SomeProject): Iterator[Field] = fields.iterator
    override def contains(f: Field)(implicit p: SomeProject): Boolean = fields.contains(f)
    override def priority: Int = 1
}

class TypeBasedFieldMatcher(val fieldType: FieldType) extends FieldMatcher {

    override def initialFields(implicit p: SomeProject): Iterator[Field] = p.allFields.iterator.filter(_.fieldType == fieldType)
    override def contains(f: Field)(implicit p: SomeProject): Boolean = f.fieldType == fieldType
    override def priority: Int = 3
}

class ActualReceiverBasedFieldMatcher(val receiver: IsReferenceValue) extends FieldMatcher {

    override def initialFields(implicit p: SomeProject): Iterator[Field] =
        p.allClassFiles.iterator.flatMap { _.fields.filter(contains) }

    override def contains(f: Field)(implicit p: SomeProject): Boolean = {
        val isNull = receiver.isNull
        (isNull.isNoOrUnknown && receiver.isValueASubtypeOf(f.classFile.thisType)(p.classHierarchy).isYesOrUnknown) ||
            (isNull.isYesOrUnknown && f.isStatic)
    }

    override def priority: Int = 3
}

class ActualParameterBasedFieldMatcher(val actualParam: V) extends FieldMatcher {
    override def initialFields(implicit p: SomeProject): Iterator[Field] =
        p.allClassFiles.iterator.flatMap { _.fields.filter(contains) }

    override def contains(f: Field)(implicit p: SomeProject): Boolean = {
        implicit val ch: ClassHierarchy = p.classHierarchy

        (f.fieldType, actualParam.value) match {
            // the actual type is null and the declared type is a ref type
            case (_: ReferenceType, _: IsNullValue) =>
                true

            // declared type and actual type are reference types and assignable
            case (pType: ReferenceType, v: IsReferenceValue) =>
                v.isValueASubtypeOf(pType).isYesOrUnknown

            // both declared type and actual type are integer like
            // IMPROVE: This can be removed when AI is powerful enough to find out the intended types of integer like values
            case (_: CTIntType, _: IsIntegerLikeValue[_]) =>
                true

            // declared type and actual type are base types and the same type
            case (pType: BaseType, v: IsPrimitiveValue[_]) =>
                v.primitiveType eq pType

            // the actual type is null and the declared type is a base type
            case (_: BaseType, _: IsNullValue) =>
                false

            // declared type is base type, actual type might be a boxed value
            case (pType: BaseType, v: IsReferenceValue) => // TODO this for int like special case
                v.asReferenceValue.isValueASubtypeOf(pType.WrapperType).isYesOrUnknown

            // actual type is base type, declared type might be a boxed type
            case (pType: ObjectType, _: IsIntegerLikeValue[_]) =>
                pType.isPrimitiveTypeWrapper && ObjectType.primitiveType(pType).get.isInstanceOf[IntLikeType]

            // actual type is base type, declared type might be a boxed type
            case (pType: ObjectType, v: IsPrimitiveValue[_]) =>
                pType.isPrimitiveTypeWrapperOf(v.primitiveType)

            case _ =>
                false
        }
    }

    override def priority: Int = 3
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

object PrivateFieldMatcher extends PropertyBasedFieldMatcher {

    override def contains(f: Field)(implicit p: SomeProject): Boolean = f.isPrivate
}

object PublicFieldMatcher extends PropertyBasedFieldMatcher {

    override def contains(f: Field)(implicit p: SomeProject): Boolean = f.isPublic
}

object AllFieldsMatcher extends FieldMatcher {

    override def initialFields(implicit p: SomeProject): Iterator[Field] = p.allFields.iterator
    override def contains(f: Field)(implicit p: SomeProject): Boolean = true
    override def priority: Int = 5
}

object NoFieldsMatcher extends FieldMatcher {

    override def initialFields(implicit p: SomeProject): Iterator[Field] = Iterator.empty
    override def contains(f: Field)(implicit p: SomeProject): Boolean = false
    override def priority: Int = 0
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
