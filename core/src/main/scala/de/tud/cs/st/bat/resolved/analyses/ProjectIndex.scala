/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st
package bat
package resolved
package analyses

import scala.collection.Set
import scala.collection.Map

/**
 * An index of the source elements (methods and fields) of a project. The index
 * enables efficient lookup of source elements (methods and fields) given the
 * methods/fields name and the descriptor/field type.
 *
 * To get an instance of a project index call [[Project.get]] and pass in
 * the [[ProjectIndexKey]] object.
 *
 * @author Michael Eichberg
 */
class ProjectIndex private (
        val fields: Map[String, Map[FieldType, Iterable[Field]]],
        val methods: Map[String, Map[MethodDescriptor, Iterable[Method]]]) {

    def findFields(name: String, fieldType: FieldType): Iterable[Field] =
        fields.get(name).flatMap(_.get(fieldType)).getOrElse(Iterable.empty)

    def findFields(name: String): Iterable[Field] =
        fields.get(name).map(_.values.flatten).getOrElse(Iterable.empty)

    def findMethods(name: String, descriptor: MethodDescriptor): Iterable[Method] =
        methods.get(name).flatMap(_.get(descriptor)).getOrElse(Iterable.empty)

    def findMethods(name: String): Iterable[Method] =
        methods.get(name).map(_.values.flatten).getOrElse(Iterable.empty)

    def statistics(): Map[String, Any] = {

        def getMostOftenUsed(elementsWithSharedName: Iterable[(String, Map[_, Iterable[ClassMember]])]) = {
            elementsWithSharedName.foldLeft((0, Set.empty[String])) { (c, n) ⇒
                val nName = n._1
                val nSize = n._2.size
                if (c._1 < nSize)
                    (nSize, Set(nName))
                else if (c._1 == nSize)
                    (nSize, c._2 + n._1)
                else
                    c
            }
        }

        val fieldsWithSharedName = fields.view.filter(_._2.size > 1)
        val mostOftenUsedFieldName = getMostOftenUsed(fieldsWithSharedName)

        val methodsWithSharedName =
            methods.view.filter { kv ⇒
                kv._1 != "<init>" && kv._1 != "<clinit>" && kv._2.size > 1
            }
        val mostOftenUsedMethodName =
            methodsWithSharedName.foldLeft((0, Set.empty[String])) { (c, n) ⇒
                val nName = n._1
                val nSize = n._2.size
                if (c._1 < nSize)
                    (nSize, Set(nName))
                else if (c._1 == nSize)
                    (nSize, c._2 + n._1)
                else
                    c
            }

        Map(
            "number of field names that are used more than once" ->
                fieldsWithSharedName.size,
            "number of fields that share the same name and type" ->
                fieldsWithSharedName.filter(_._2.size > 2).size,
            "number of usages of the most often used field name" ->
                mostOftenUsedFieldName._1,
            "the most often used field name" ->
                mostOftenUsedFieldName._2.mkString(", "),
            "number of method names that are used more than once (constructors are filtered)" ->
                methodsWithSharedName.size,
            "number of methods that share the same signature (constructors are filtered)" ->
                methodsWithSharedName.filter(_._2.size > 2).size,
            "number of usages of the most often used method name (constructors are filtered)" ->
                mostOftenUsedMethodName._1,
            "the most often used method name (constructors are filtered)" ->
                mostOftenUsedMethodName._2.mkString(", ")
        )
    }
}

/**
 * Factory for [[ProjectIndex]] objects.
 *
 * @author Michael Eichberg
 */
object ProjectIndex {

    def apply(project: SomeProject): ProjectIndex = {

        import scala.collection.mutable.HashMap

        import scala.concurrent.{ future, Future, Await, ExecutionContext }
        import scala.concurrent.duration.Duration
        import ExecutionContext.Implicits.global

        def InitialHashMap[K, V](estimatedSize: Int): HashMap[K, V] = {
            new HashMap[K, V] {
                override protected def initialSize: Int = estimatedSize
            }
        }

        val fieldsFuture: Future[HashMap[String, HashMap[FieldType, List[Field]]]] = future {
            val fields = InitialHashMap[String, HashMap[FieldType, List[Field]]](project.fields.size * 2 / 3)
            for (field ← project.fields) {
                val fieldName = field.name
                val fieldType = field.fieldType
                fields.get(fieldName) match {
                    case None ⇒
                        val fieldTypeToField = InitialHashMap[FieldType, List[Field]](4)
                        fieldTypeToField.update(fieldType, List(field))
                        fields.update(fieldName, fieldTypeToField)
                    case Some(fieldTypeToField) ⇒
                        fieldTypeToField.get(fieldType) match {
                            case None ⇒
                                fieldTypeToField.put(fieldType, List(field))
                            case Some(theFields) ⇒
                                fieldTypeToField.put(fieldType, field :: theFields)
                        }
                }
            }
            fields
        }

        val methods: HashMap[String, HashMap[MethodDescriptor, List[Method]]] = {
            val methods = InitialHashMap[String, HashMap[MethodDescriptor, List[Method]]](project.methods.size * 2 / 3)
            for (method ← project.methods) {
                val methodName = method.name
                val methodDescriptor = method.descriptor
                methods.get(methodName) match {
                    case None ⇒
                        val descriptorToField = InitialHashMap[MethodDescriptor, List[Method]](4)
                        descriptorToField.update(methodDescriptor, List(method))
                        methods.update(methodName, descriptorToField)
                    case Some(descriptorToField) ⇒
                        descriptorToField.get(methodDescriptor) match {
                            case None ⇒
                                descriptorToField.put(methodDescriptor, List(method))
                            case Some(theMethods) ⇒
                                descriptorToField.put(methodDescriptor, method :: theMethods)
                        }
                }
            }
            methods
        }

        new ProjectIndex(Await.result(fieldsFuture, Duration.Inf), methods)
    }

}




