/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br

import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.collection.mutable

/**
 * Represents a declared field of a class identified by [[declaringClassType]];
 * that is, a field which belongs to the API of the class itself or a super class thereof.
 *
 * @author Maximilian Rüsch
 */
sealed abstract class DeclaredField {

    /**
     * The declaring type; the returned type may not define the field; it could be defined by
     * one or more super classes/interfaces in case of Java 8+.
     */
    def declaringClassType: ObjectType
    def name: String
    def fieldType: FieldType
    def toJava: String = s"${declaringClassType.toJava}{ ${fieldType.toJava} }"

    def asDefinedField: DefinedField
    def definedField: Field

    /**
     * A unique ID.
     */
    val id: Int

    override def equals(other: Any): Boolean = other match {
        case that: DeclaredMethod => id == that.id
        case _                    => false
    }

    override def hashCode(): Int = id
}

/**
 * Represents a declared method; i.e., a method which belongs to the (public and private) API of a
 * class along with a reference to the original declaration.
 */
final class DefinedField private[br] (
        override val id:           Int,
        override val definedField: Field
) extends DeclaredField {

    override def declaringClassType: ObjectType = definedField.declaringClassFile.thisType
    override def name: String = definedField.name
    override def fieldType: FieldType = definedField.fieldType

    override def asDefinedField: DefinedField = this

    override def toString: String = {
        s"DefinedField(declaringClassType=${declaringClassType.toJava},definedField=${definedField.toJava})"
    }
}

object DefinedFieldsKey extends ProjectInformationKey[DefinedFields, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq.empty

    override def compute(p: SomeProject): DefinedFields = new DefinedFields()
}

class DefinedFields {

    @volatile private var id2definedField = new Array[DefinedField](32768)
    private val definedField2id = new mutable.HashMap[Field, DefinedField]()

    private val nextId = new AtomicInteger(1)
    private val rwLock = new ReentrantReadWriteLock()

    def apply(id: Int): DefinedField = {
        id2definedField(id)
    }

    def apply(field: Field): DefinedField = {
        val readLock = rwLock.readLock()
        readLock.lock()
        try {
            val field0 = definedField2id.get(field)
            if (field0.isDefined) return field0.get;
        } finally {
            readLock.unlock()
        }

        val writeLock = rwLock.writeLock()
        writeLock.lock()
        try {
            val field0 = definedField2id.get(field)
            if (field0.isDefined) return field0.get;

            val definedField = new DefinedField(nextId.getAndIncrement(), field)
            definedField2id.put(field, definedField)
            val curMap = id2definedField
            if (definedField.id < curMap.length) {
                curMap(definedField.id) = definedField
            } else {
                val newMap = java.util.Arrays.copyOf(curMap, curMap.length * 2)
                newMap(definedField.id) = definedField
                id2definedField = newMap
            }
            definedField
        } finally {
            writeLock.unlock()
        }
    }
}
