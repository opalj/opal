/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock

import org.opalj.br.instructions.FieldAccess
import org.opalj.log.OPALLogger.info

class DeclaredFields(
    private[this] val project:          SomeProject,
    private[this] var id2declaredField: Array[DeclaredField],
    private[this] val declaredInformation2id: ConcurrentHashMap[
        ObjectType,
        ConcurrentHashMap[String, ConcurrentHashMap[FieldType, DeclaredField]]
    ],
    private[this] val nextId: AtomicInteger
) {
    private var extensionSize = 1000
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

    def apply(access: FieldAccess): DeclaredField = {
        project.resolveFieldReference(access) match {
            case Some(field) => apply(field)
            case None        => apply(access.declaringClass, access.name, access.fieldType)
        }
    }

    def apply(
        declaringClassType: ObjectType,
        name:               String,
        fieldType:          FieldType
    ): DeclaredField = {
        project.resolveFieldReference(declaringClassType, name, fieldType) match {
            case Some(field) => apply(field)
            case None => getDeclaredField(
                    declaringClassType,
                    name,
                    fieldType,
                    id => new VirtualDeclaredField(declaringClassType, name, fieldType, id)
                )
        }
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
            val declaredField = declaredInformation2id
                .computeIfAbsent(declaringClassType, _ => new ConcurrentHashMap)
                .computeIfAbsent(name, _ => new ConcurrentHashMap)
                .get(fieldType)
            if (declaredField != null) return declaredField;
        } finally {
            readLock.unlock()
        }

        val writeLock = rwLock.writeLock()
        writeLock.lock()
        try {
            val type2field = declaredInformation2id
                .computeIfAbsent(declaringClassType, _ => new ConcurrentHashMap)
                .computeIfAbsent(name, _ => new ConcurrentHashMap)
            val declaredField = type2field.get(fieldType)
            if (declaredField != null) return declaredField;

            val newDeclaredField = declaredFieldFactory(nextId.getAndIncrement())
            type2field.put(fieldType, newDeclaredField)
            if (id2declaredField.size <= newDeclaredField.id) {
                info(
                    "project",
                    "too many declared fields; extended the underlying array"
                )(project.logContext)
                val id2declaredFieldExt = new Array[DeclaredField](id2declaredField.length + extensionSize)
                extensionSize = Math.min(extensionSize * 2, 32000)
                Array.copy(id2declaredField, 0, id2declaredFieldExt, 0, id2declaredField.length)
                id2declaredField = id2declaredFieldExt
            }
            id2declaredField(newDeclaredField.id) = newDeclaredField

            newDeclaredField
        } finally {
            writeLock.unlock()
        }
    }
}
