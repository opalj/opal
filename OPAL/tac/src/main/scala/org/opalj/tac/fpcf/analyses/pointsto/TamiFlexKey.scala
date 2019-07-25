/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable
import scala.io.Source

import org.opalj.br.ArrayType
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.FieldType
import org.opalj.br.ReferenceType

class TamiFlexLogData(
        private[this] val _newInstance:  scala.collection.Map[(String /*Method*/ , Int /*Line Number*/ ), scala.collection.Set[ReferenceType]],
        private[this] val _methodInvoke: scala.collection.Map[(String /*Method*/ , Int /*Line Number*/ ), scala.collection.Set[DeclaredMethod]],
        private[this] val _fields:       scala.collection.Map[(String /*Method*/ , Int /*Line Number*/ ), scala.collection.Set[Field]],
        private[this] val _arrays:       scala.collection.Map[(String /*Method*/ , Int /*Line Number*/ ), scala.collection.Set[ArrayType]],
        private[this] val _forNames:     scala.collection.Map[(String /*Method*/ , Int /*Line Number*/ ), scala.collection.Set[ObjectType]]
) {
    private[this] def toMethodDesc(method: DeclaredMethod): String = {
        s"${method.declaringClassType.toJava}.${method.name}"
    }

    def newInstance(source: DeclaredMethod, sourceLine: Int): scala.collection.Set[ReferenceType] = {
        val sourceDesc = toMethodDesc(source)
        val key = (sourceDesc, sourceLine)
        if (_newInstance.contains(key))
            _newInstance(key)
        else {
            val fallbackKey = (sourceDesc, -1)
            if (_newInstance.contains(fallbackKey))
                _newInstance(fallbackKey)
            else {
                _newInstance.getOrElse(("", -1), Set.empty)
            }
        }
    }

    def methodInvokes(source: DeclaredMethod, sourceLine: Int): scala.collection.Set[DeclaredMethod] = {
        val sourceDesc = toMethodDesc(source)
        val key = (sourceDesc, sourceLine)
        if (_methodInvoke.contains(key))
            _methodInvoke(key)
        else {
            val fallbackKey = (sourceDesc, -1)
            if (_methodInvoke.contains(fallbackKey))
                _methodInvoke(fallbackKey)
            else {
                _methodInvoke.getOrElse(("", -1), Set.empty)
            }
        }
    }

    def fields(source: DeclaredMethod, sourceLine: Int): scala.collection.Set[Field] = {
        val sourceDesc = toMethodDesc(source)
        val key = (sourceDesc, sourceLine)
        if (_fields.contains(key))
            _fields(key)
        else {
            val fallbackKey = (sourceDesc, -1)
            if (_fields.contains(fallbackKey))
                _fields(fallbackKey)
            else {
                _fields.getOrElse(("", -1), Set.empty)
            }
        }
    }

    def arrays(source: DeclaredMethod, sourceLine: Int): scala.collection.Set[ArrayType] = {
        val sourceDesc = toMethodDesc(source)
        val key = (sourceDesc, sourceLine)
        if (_arrays.contains(key))
            _arrays(key)
        else {
            val fallbackKey = (sourceDesc, -1)
            if (_arrays.contains(fallbackKey))
                _arrays(fallbackKey)
            else {
                _arrays.getOrElse(("", -1), Set.empty)
            }
        }
    }

    def forNames(source: DeclaredMethod, sourceLine: Int): scala.collection.Set[ObjectType] = {
        val sourceDesc = toMethodDesc(source)
        val key = (sourceDesc, sourceLine)
        if (_forNames.contains(key))
            _forNames(key)
        else {
            val fallbackKey = (sourceDesc, -1)
            if (_forNames.contains(fallbackKey))
                _forNames(fallbackKey)
            else {
                _forNames.getOrElse(("", -1), Set.empty)
            }
        }
    }
}

/**
 * Stores TamiFlex log information for the current project
 *
 * @author Florian Kuebler
 */
object TamiFlexKey extends ProjectInformationKey[TamiFlexLogData, Nothing] {
    val configKey = "org.opalj.tac.fpcf.analyses.pointsto.TamiFlex.logFile"

    override protected def requirements: ProjectInformationKeys = {
        Seq(DeclaredMethodsKey)
    }

    override protected def compute(project: SomeProject): TamiFlexLogData = {
        implicit val declaredMethods = project.get(DeclaredMethodsKey)
        val newInstance: mutable.Map[(String /*Method*/ , Int /*Line Number*/ ), mutable.Set[ReferenceType]] = mutable.Map.empty
        val methodInvoke: mutable.Map[(String /*Method*/ , Int /*Line Number*/ ), mutable.Set[DeclaredMethod]] = mutable.Map.empty
        val fields: mutable.Map[(String /*Method*/ , Int /*Line Number*/ ), mutable.Set[Field]] = mutable.Map.empty
        val arrays: mutable.Map[(String /*Method*/ , Int /*Line Number*/ ), mutable.Set[ArrayType]] = mutable.Map.empty
        val forNames: mutable.Map[(String /*Method*/ , Int /*Line Number*/ ), mutable.Set[ObjectType]] = mutable.Map.empty

        if (project.config.hasPath(configKey)) {
            val logName = project.config.getString(configKey)
            Source.fromFile(logName).getLines().foreach { line ⇒
                line.split(";", -1) match {
                    case Array("Array.newInstance", arrayType, sourceMethod, sourceLine, _, _) ⇒
                        val line = if (sourceLine == "") -1 else sourceLine.toInt
                        val oldSet = newInstance.getOrElseUpdate((sourceMethod, line), mutable.Set.empty)
                        oldSet.add(FieldType(toJVMType(arrayType)).asArrayType)

                    case Array("Array.get*", arrayType, sourceMethod, sourceLine, _, _) ⇒
                        val line = if (sourceLine == "") -1 else sourceLine.toInt
                        val oldSet = arrays.getOrElseUpdate((sourceMethod, line), mutable.Set.empty)
                        oldSet.add(FieldType(toJVMType(arrayType)).asArrayType)

                    case Array("Array.set*", arrayType, sourceMethod, sourceLine, _, _) ⇒
                        val line = if (sourceLine == "") -1 else sourceLine.toInt
                        val oldSet = arrays.getOrElseUpdate((sourceMethod, line), mutable.Set.empty)
                        oldSet.add(FieldType(toJVMType(arrayType)).asArrayType)

                    case Array("Class.forName", classType, sourceMethod, sourceLine, _, _) ⇒
                        val line = if (sourceLine == "") -1 else sourceLine.toInt
                        val oldSet = forNames.getOrElseUpdate((sourceMethod, line), mutable.Set.empty)
                        oldSet.add(ObjectType(toJVMType(classType)))

                    case Array("Class.getDeclaredField", field, sourceMethod, sourceLine, _, _)           ⇒
                    // TODO: Handle this case

                    case Array("Class.getDeclaredFields", declaringClass, sourceMethod, sourceLine, _, _) ⇒
                    // TODO: Handle this case

                    case Array("Class.getDeclaredMethod", method, sourceMethod, sourceLine, _, _)         ⇒
                    // TODO: Handle this case

                    case Array("Class.getDeclaredMethod", declaringClass, sourceMethod, sourceLine, _, _) ⇒
                    // TODO: Handle this case

                    case Array("Class.getMethod", method, sourceMethod, sourceLine, _, _)                 ⇒
                    // TODO: Handle this case

                    case Array("Class.getMethods", declaringClass, sourceMethod, sourceLine, _, _)        ⇒
                    // TODO: Handle this case

                    case Array("Class.newInstance", instantiatedTypeDesc, sourceMethod, sourceLine, _, _) ⇒
                        val line = if (sourceLine == "") -1 else sourceLine.toInt
                        val instantiatedType = ObjectType(toJVMType(instantiatedTypeDesc))
                        val oldSet = newInstance.getOrElseUpdate((sourceMethod, line), mutable.Set.empty)
                        oldSet.add(instantiatedType)
                        val oldInvokes = methodInvoke.getOrElseUpdate((sourceMethod, line), mutable.Set.empty)
                        oldInvokes.add(
                            declaredMethods(
                                instantiatedType,
                                "",
                                instantiatedType,
                                "<init>",
                                MethodDescriptor.NoArgsAndReturnVoid
                            )
                        )
                    case Array("Constructor.getModifiers", constructor, sourceMethod, sourceLine, _, _) ⇒
                    // TODO: Handle this case

                    case Array("Constructor.newInstance", constructorDesc, sourceMethod, sourceLine, _, _) ⇒
                        val line = if (sourceLine == "") -1 else sourceLine.toInt
                        val constructor = toDeclaredMethod(constructorDesc)
                        val oldSet = newInstance.getOrElseUpdate((sourceMethod, line), mutable.Set.empty)
                        oldSet.add(constructor.declaringClassType)
                        val oldInvokes = methodInvoke.getOrElseUpdate((sourceMethod, line), mutable.Set.empty)
                        oldInvokes.add(constructor)

                    case Array("Field.getDeclaringClass", field, sourceMethod, sourceLine, _, _) ⇒
                    // TODO: Handle this case

                    case Array("Field.getModifiers", field, sourceMethod, sourceLine, _, _)      ⇒
                    // TODO: Handle this case

                    case Array("Field.getName", field, sourceMethod, sourceLine, _, _)           ⇒
                    // TODO: Handle this case

                    case Array("Field.get*", fieldDesc, sourceMethod, sourceLine, _, _) ⇒
                        val line = if (sourceLine == "") -1 else sourceLine.toInt
                        val oldSet = fields.getOrElseUpdate((sourceMethod, line), mutable.Set.empty)
                        val field = toField(fieldDesc, project)
                        if (field.isDefined)
                            oldSet.add(field.get)

                    case Array("Field.set*", fieldDesc, sourceMethod, sourceLine, _, _) ⇒
                        val line = if (sourceLine == "") -1 else sourceLine.toInt
                        val oldSet = fields.getOrElseUpdate((sourceMethod, line), mutable.Set.empty)
                        val field = toField(fieldDesc, project)
                        if (field.isDefined)
                            oldSet.add(field.get)

                    case Array("Method.getDeclaringClass", method, sourceMethod, sourceLine, _, _) ⇒
                    // TODO: Handle this case

                    case Array("Method.getModifiers", method, sourceMethod, sourceLine, _, _)      ⇒
                    // TODO: Handle this case

                    case Array("Method.getName", method, sourceMethod, sourceLine, _, _)           ⇒
                    // TODO: Handle this case

                    case Array("Method.invoke", methodDesc, sourceMethod, sourceLine, _, _) ⇒
                        val line = if (sourceLine == "") -1 else sourceLine.toInt
                        val method = toDeclaredMethod(methodDesc)
                        val oldInvokes = methodInvoke.getOrElseUpdate((sourceMethod, line), mutable.Set.empty)
                        oldInvokes.add(method)

                    case e ⇒ throw new RuntimeException(s"unexpected log entry ${e.mkString(",")}")

                }
            }

        }

        new TamiFlexLogData(newInstance, methodInvoke, fields, arrays, forNames)
    }

    private[this] def toJVMType(javaType: String): String = {
        val trimmedType = javaType.trim
        if (trimmedType.endsWith("[]")) "["+toJVMType(trimmedType.substring(0, trimmedType.length - 2))
        else trimmedType match {
            case "void"    ⇒ "V"
            case "byte"    ⇒ "B"
            case "char"    ⇒ "C"
            case "double"  ⇒ "D"
            case "float"   ⇒ "F"
            case "int"     ⇒ "I"
            case "long"    ⇒ "J"
            case "short"   ⇒ "S"
            case "boolean" ⇒ "Z"
            case _         ⇒ "L"+trimmedType.replace('.', '/')+";"
        }
    }

    private[this] def toDeclaredMethod(
        methodDesc: String
    )(implicit declaredMethods: DeclaredMethods): DeclaredMethod = {
        val regex = "<([^:]+): ([^ ]+) ([^(]+)\\(([^)]*)\\)>".r
        methodDesc match {
            case regex(declaringClass, returnType, name, parameterTypes) ⇒
                val declaringClassType = ObjectType(toJVMType(declaringClass))
                val jvmSignature = parameterTypes.split(',').map(toJVMType).mkString("(", "", ")"+toJVMType(returnType))
                declaredMethods(declaringClassType, "", declaringClassType, name, MethodDescriptor(jvmSignature))
        }

    }

    private[this] def toField(fieldDesc: String, project: SomeProject): Option[Field] = {
        val regex = "<([^:]+): ([^ ]+) ([^>]+)>".r
        fieldDesc match {
            case regex(declaringClass, fieldTypeDesc, name) ⇒
                val declaringClassType = ObjectType(toJVMType(declaringClass))
                val fieldType = FieldType(toJVMType(fieldTypeDesc))
                project.resolveFieldReference(declaringClassType, name, fieldType)
        }
    }
}
