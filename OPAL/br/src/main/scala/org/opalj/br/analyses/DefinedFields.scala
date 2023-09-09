/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import scala.collection.mutable

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
