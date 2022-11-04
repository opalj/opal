/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import scala.collection.Map
import scala.collection.mutable

/**
 * An index that enables the efficient lookup of source elements (methods and fields)
 * given the method's/field's name and the descriptor/field type. The index contains fields
 * with public, protected, `<default>` and private visibility.
 *
 * Basically an index of the source elements (methods and fields) of a project.
 *
 * This index can be used, e.g., to resolve method calls based on the method's names and/or
 * descriptors.
 *
 * To get an instance of a project index call [[Project.get]] and pass in
 * the [[ProjectIndexKey]] object.
 *
 * @see [[FieldAccessInformation]] to get the information where a field is accessed.
 *
 * @author Michael Eichberg
 */
class ProjectIndex private (
        val fields:  Map[String, Map[FieldType, List[Field]]],
        val methods: Map[String, Map[MethodDescriptor, List[Method]]]
) {

    def findFields(name: String, fieldType: FieldType): List[Field] = {
        fields.get(name).flatMap(_.get(fieldType)).getOrElse(Nil)
    }

    def findFields(name: String): Iterable[Field] = {
        fields.get(name).map(_.values.flatten).getOrElse(Nil)
    }

    def findMethods(name: String, descriptor: MethodDescriptor): List[Method] = {
        methods.get(name).flatMap(_.get(descriptor)).getOrElse(Nil)
    }

    def findMethods(name: String): Iterable[Method] = {
        methods.get(name).map(_.values.flatten).getOrElse(Nil)
    }

    /**
     * Returns a map of some basic statistical information, such as the most often used
     * field/method name.
     */
    def statistics(): Map[String, Any] = {

        def getMostOftenUsed(
            elementsWithSharedName: Iterable[(String, Map[_, Iterable[ClassMember]])]
        ) = {
            elementsWithSharedName.foldLeft((0, mutable.Set.empty[String])) { (c, n) =>
                val nName = n._1
                val nSize = n._2.size
                if (c._1 < nSize)
                    (nSize, mutable.Set(nName))
                else if (c._1 == nSize)
                    (nSize, c._2.addOne(n._1))
                else
                    c
            }
        }

        val fieldsWithSharedName = fields.view.filter(_._2.size > 1)
        val mostOftenUsedFieldName = getMostOftenUsed(fieldsWithSharedName)

        val methodsWithSharedName =
            methods.view.filter(kv => kv._1 != "<init>" && kv._1 != "<clinit>" && kv._2.size > 1)
        val mostOftenUsedMethodName = getMostOftenUsed(methodsWithSharedName)

        Map(
            "number of field names that are used more than once" ->
                fieldsWithSharedName.size,
            "number of fields that share the same name and type" ->
                fieldsWithSharedName.count(_._2.size > 2),
            "number of usages of the most often used field name" ->
                mostOftenUsedFieldName._1,
            "the most often used field name" ->
                mostOftenUsedFieldName._2.mkString(", "),
            "number of method names that are used more than once (initializers are filtered)" ->
                methodsWithSharedName.size,
            "number of methods that share the same signature (initializers are filtered)" ->
                methodsWithSharedName.count(_._2.size > 2),
            "number of usages of the most often used method name (initializers are filtered)" ->
                mostOftenUsedMethodName._1,
            "the most often used method name (initializers are filtered)" ->
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

        import scala.collection.mutable.AnyRefMap

        import scala.concurrent.{Future, Await, ExecutionContext}
        import scala.concurrent.duration.Duration
        import ExecutionContext.Implicits.global

        val fieldsFuture: Future[AnyRefMap[String, AnyRefMap[FieldType, List[Field]]]] = Future {
            val estimatedFieldsCount = project.fieldsCount
            val fields = new AnyRefMap[String, AnyRefMap[FieldType, List[Field]]](estimatedFieldsCount)
            for (field <- project.allFields) {
                val fieldName = field.name
                val fieldType = field.fieldType
                fields.get(fieldName) match {
                    case None =>
                        val fieldTypeToField = new AnyRefMap[FieldType, List[Field]](4)
                        fieldTypeToField.update(fieldType, List(field))
                        fields.update(fieldName, fieldTypeToField)
                    case Some(fieldTypeToField) =>
                        fieldTypeToField.get(fieldType) match {
                            case None =>
                                fieldTypeToField.put(fieldType, List(field))
                            case Some(theFields) =>
                                fieldTypeToField.put(fieldType, field :: theFields)
                        }
                }
            }
            fields.foreachValue(_.repack())
            fields.repack()
            fields
        }

        val methods: AnyRefMap[String, AnyRefMap[MethodDescriptor, List[Method]]] = {
            val estimatedMethodsCount = project.methodsCount
            val methods = new AnyRefMap[String, AnyRefMap[MethodDescriptor, List[Method]]](estimatedMethodsCount)
            for (method <- project.allMethods) {
                val methodName = method.name
                val methodDescriptor = method.descriptor
                methods.get(methodName) match {
                    case None =>
                        val descriptorToField = new AnyRefMap[MethodDescriptor, List[Method]](4)
                        descriptorToField.update(methodDescriptor, List(method))
                        methods.update(methodName, descriptorToField)
                    case Some(descriptorToField) =>
                        descriptorToField.get(methodDescriptor) match {
                            case None =>
                                descriptorToField.put(methodDescriptor, List(method))
                            case Some(theMethods) =>
                                descriptorToField.put(methodDescriptor, method :: theMethods)
                        }
                }
            }
            methods.foreachValue(_.repack())
            methods.repack()
            methods
        }

        new ProjectIndex(Await.result(fieldsFuture, Duration.Inf), methods)
    }

}
