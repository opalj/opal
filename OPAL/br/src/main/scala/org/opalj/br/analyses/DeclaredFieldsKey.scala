/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters.EnumerationHasAsScala

object DeclaredFieldsKey extends ProjectInformationKey[DeclaredFields, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq.empty

    override def compute(p: SomeProject): DeclaredFields = {
        val idCounter = new AtomicInteger()
        val result: ConcurrentHashMap[ObjectType, ConcurrentHashMap[String, ConcurrentHashMap[FieldType, DeclaredField]]] =
            new ConcurrentHashMap

        p.allFields.iterator.foreach { f =>
            result
                .computeIfAbsent(f.declaringClassFile.thisType, _ => new ConcurrentHashMap())
                .computeIfAbsent(f.name, _ => new ConcurrentHashMap())
                .put(f.fieldType, new DefinedField(idCounter.getAndIncrement(), f))
        }

        val id2field = new Array[DeclaredField](idCounter.get() + 1000)
        for {
            name2TypeFieldMapping <- result.elements().asScala
            type2Field <- name2TypeFieldMapping.elements().asScala
            field <- type2Field.elements().asScala
        } {
            id2field(field.id) = field
        }

        new DeclaredFields(p, id2field, result, idCounter)
    }
}
