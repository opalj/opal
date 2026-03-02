/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.util.concurrent.ConcurrentHashMap as ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger
import scala.jdk.CollectionConverters.EnumerationHasAsScala

object DeclaredFieldsKey extends ProjectInformationKey[DeclaredFields, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq.empty

    override def compute(p: SomeProject): DeclaredFields = {
        val idCounter = new AtomicInteger()
        val result: ConcurrentMap[ClassType, ConcurrentMap[String, ConcurrentMap[FieldType, DeclaredField]]] =
            new ConcurrentMap

        p.allFields.iterator.foreach { f =>
            result
                .computeIfAbsent(f.declaringClassFile.thisType, _ => new ConcurrentMap())
                .computeIfAbsent(f.name, _ => new ConcurrentMap())
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
