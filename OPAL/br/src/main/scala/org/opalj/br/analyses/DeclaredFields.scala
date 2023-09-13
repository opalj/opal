/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.collection.mutable

class DeclaredFields {

    @volatile private var id2declaredField = new Array[DeclaredField](32768)
    private val declaredInformation2id = new mutable.HashMap[ObjectType, mutable.HashMap[String, mutable.HashMap[FieldType, DeclaredField]]]()

    private val nextId = new AtomicInteger(1)
    private val rwLock = new ReentrantReadWriteLock()

    def apply(id: Int): DeclaredField = {
        id2declaredField(id)
    }

    def apply(field: Field): DefinedField = {
        getDeclaredField(
            field.declaringClassFile.thisType,
            field.name,
            field.fieldType,
            id => new DefinedField(id, field)
        ).asDefinedField
    }

    def apply(
        declaringClassType: ObjectType,
        name:               String,
        fieldType:          FieldType
    ): DeclaredField = {
        getDeclaredField(
            declaringClassType,
            name,
            fieldType,
            id => new VirtualDeclaredField(declaringClassType, name, fieldType, id)
        )
    }

    private def getDeclaredField(
        declaringClassType:   ObjectType,
        name:                 String,
        fieldType:            FieldType,
        declaredFieldFactory: Int => DeclaredField
    ): DeclaredField = {
        val readLock = rwLock.readLock()
        readLock.lock()
        try {
            val declaredField = declaredInformation2id.get(declaringClassType).flatMap(_.get(name)).flatMap(_.get(fieldType))
            if (declaredField.isDefined) return declaredField.get;
        } finally {
            readLock.unlock()
        }

        val writeLock = rwLock.writeLock()
        writeLock.lock()
        try {
            val declaredField = declaredInformation2id.get(declaringClassType).flatMap(_.get(name)).flatMap(_.get(fieldType))
            if (declaredField.isDefined) return declaredField.get;

            val newDeclaredField = declaredFieldFactory(nextId.getAndIncrement())
            declaredInformation2id
                .getOrElseUpdate(declaringClassType, mutable.HashMap.empty)
                .getOrElseUpdate(name, mutable.HashMap.empty)
                .put(fieldType, newDeclaredField)

            val curMap = id2declaredField
            if (newDeclaredField.id < curMap.length) {
                curMap(newDeclaredField.id) = newDeclaredField
            } else {
                val newMap = java.util.Arrays.copyOf(curMap, curMap.length * 2)
                newMap(newDeclaredField.id) = newDeclaredField
                id2declaredField = newMap
            }
            newDeclaredField
        } finally {
            writeLock.unlock()
        }
    }
}
