/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package pointsto

import scala.collection.mutable
import scala.io.Source

import org.opalj.log.OPALLogger
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.FieldType
import org.opalj.br.ReferenceType

/**
 * Container class, to represent a tamiflex log:
 * https://github.com/secure-software-engineering/tamiflex
 *
 * @author Florian Kuebler
 */
class TamiFlexLogData(
        private[this] val _classes: scala.collection.Map[(String /*Caller*/ , String /*Reflection Method*/ , Int /*Line Number*/ ), scala.collection.Set[ReferenceType]],
        private[this] val _methods: scala.collection.Map[(String /*Caller*/ , String /*Reflection Method*/ , Int /*Line Number*/ ), scala.collection.Set[DeclaredMethod]],
        private[this] val _fields:  scala.collection.Map[(String /*Caller*/ , String /*Reflection Method*/ , Int /*Line Number*/ ), scala.collection.Set[Field]]
) {
    private[this] def toMethodDesc(method: DeclaredMethod): String = {
        s"${method.declaringClassType.toJava}.${method.name}"
    }

    def classes(source: DeclaredMethod, reflectionTarget: String, sourceLine: Int): scala.collection.Set[ReferenceType] = {
        val sourceDesc = toMethodDesc(source)
        val key = (sourceDesc, reflectionTarget, sourceLine)
        if (_classes.contains(key))
            _classes(key)
        else {
            val fallbackKey = (sourceDesc, reflectionTarget, -1)
            if (_classes.contains(fallbackKey))
                _classes(fallbackKey)
            else {
                _classes.getOrElse(("", reflectionTarget, -1), Set.empty)
            }
        }
    }

    def methods(source: DeclaredMethod, reflectionTarget: String, sourceLine: Int): scala.collection.Set[DeclaredMethod] = {
        val sourceDesc = toMethodDesc(source)
        val key = (sourceDesc, reflectionTarget, sourceLine)
        if (_methods.contains(key))
            _methods(key)
        else {
            val fallbackKey = (sourceDesc, reflectionTarget, -1)
            if (_methods.contains(fallbackKey))
                _methods(fallbackKey)
            else {
                _methods.getOrElse(("", reflectionTarget, -1), Set.empty)
            }
        }
    }

    def fields(source: DeclaredMethod, reflectionTarget: String, sourceLine: Int): scala.collection.Set[Field] = {
        val sourceDesc = toMethodDesc(source)
        val key = (sourceDesc, reflectionTarget, sourceLine)
        if (_fields.contains(key))
            _fields(key)
        else {
            val fallbackKey = (sourceDesc, reflectionTarget, -1)
            if (_fields.contains(fallbackKey))
                _fields(fallbackKey)
            else {
                _fields.getOrElse(("", reflectionTarget, -1), Set.empty)
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

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq(DeclaredMethodsKey)

    override def compute(project: SomeProject): TamiFlexLogData = {
        implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
        val classes: mutable.Map[(String /*Caller*/ , String /*Reflection Method*/ , Int /*Line Number*/ ), mutable.Set[ReferenceType]] = mutable.Map.empty
        val methods: mutable.Map[(String /*Caller*/ , String /*Reflection Method*/ , Int /*Line Number*/ ), mutable.Set[DeclaredMethod]] = mutable.Map.empty
        val fields: mutable.Map[(String /*Caller*/ , String /*Reflection Method*/ , Int /*Line Number*/ ), mutable.Set[Field]] = mutable.Map.empty

        @inline def addClassType(
            classType: String, sourceMethod: String, reflectionTarget: String, sourceLine: String
        ): Unit = {
            val line = if (sourceLine == "") -1 else sourceLine.toInt
            val oldSet =
                classes.getOrElseUpdate((sourceMethod, reflectionTarget, line), mutable.Set.empty)
            oldSet.add(FieldType(toJVMType(classType)).asReferenceType)
        }

        @inline def addField(
            fieldDesc: String, sourceMethod: String, reflectionTarget: String, sourceLine: String
        ): Unit = {
            val line = if (sourceLine == "") -1 else sourceLine.toInt
            val oldSet =
                fields.getOrElseUpdate((sourceMethod, reflectionTarget, line), mutable.Set.empty)
            val field = toField(fieldDesc, project)
            if (field.isDefined)
                oldSet.add(field.get)
        }

        @inline def addMethod(
            methodDesc: String, sourceMethod: String, reflectionTarget: String, sourceLine: String
        ): Unit = {
            val line = if (sourceLine == "") -1 else sourceLine.toInt
            val method = toDeclaredMethod(methodDesc)
            val oldInvokes =
                methods.getOrElseUpdate((sourceMethod, reflectionTarget, line), mutable.Set.empty)
            oldInvokes.add(method)
        }

        // TODO: There are more methods that could probably be handled, e.g. "getAnnotations"

        if (project.config.hasPath(configKey)) {
            val logName = project.config.getString(configKey)
            OPALLogger.info("analysis configuration", s"Using tamiflex log file: $logName")(project.logContext)
            Source.fromFile(logName).getLines().foreach { line =>
                val entries = line.split(";", -1)
                entries match {
                    case Array("Array.newInstance" | "Array.get*" | "Array.set*",
                        arrayType, sourceMethod, sourceLine, _, _) =>
                        addClassType(arrayType, sourceMethod, entries.head, sourceLine)

                    case Array("Class.forName" |
                        "Class.getDeclaredConstructors" | "Class.getConstructors" |
                        "Class.getDeclaredFields" | "Class.getFields" |
                        "Class.getDeclaredMethods" | "Class.getMethods" |
                        "Class.getModifiers",
                        classType, sourceMethod, sourceLine, _, _) =>
                        addClassType(classType, sourceMethod, entries.head, sourceLine)

                    case Array("Class.getDeclaredField" | "Class.getField",
                        fieldDesc, sourceMethod, sourceLine, _, _) =>
                        addField(fieldDesc, sourceMethod, entries.head, sourceLine)

                    case Array("Class.getDeclaredConstructor" | "Class.getConstructor" |
                        "Class.getDeclaredMethod" | "Class.getMethod",
                        methodDesc, sourceMethod, sourceLine, _, _) =>
                        addMethod(methodDesc, sourceMethod, entries.head, sourceLine)

                    case Array("Class.newInstance", instantiatedTypeDesc, sourceMethod, sourceLine, _, _) =>
                        val line = if (sourceLine == "") -1 else sourceLine.toInt
                        val instantiatedType = FieldType(toJVMType(instantiatedTypeDesc)).asObjectType
                        val oldSet =
                            classes.getOrElseUpdate((sourceMethod, entries.head, line), mutable.Set.empty)
                        oldSet.add(instantiatedType)
                        val oldInvokes = methods.getOrElseUpdate((sourceMethod, entries.head, line), mutable.Set.empty)
                        val constructor = declaredMethods(
                            instantiatedType,
                            instantiatedType.packageName,
                            instantiatedType,
                            "<init>",
                            MethodDescriptor.NoArgsAndReturnVoid
                        )
                        oldInvokes.add(constructor)

                    case Array("Constructor.getModifiers", constructorDesc, sourceMethod, sourceLine, _, _) =>
                        addMethod(constructorDesc, sourceMethod, entries.head, sourceLine)

                    case Array("Constructor.newInstance", constructorDesc, sourceMethod, sourceLine, _, _) =>
                        val line = if (sourceLine == "") -1 else sourceLine.toInt
                        val constructor = toDeclaredMethod(constructorDesc)
                        val oldSet = classes.getOrElseUpdate((sourceMethod, entries.head, line), mutable.Set.empty)
                        oldSet.add(constructor.declaringClassType)
                        val oldInvokes = methods.getOrElseUpdate((sourceMethod, entries.head, line), mutable.Set.empty)
                        oldInvokes.add(constructor)

                    case Array("Field.getDeclaringClass" | "Field.getModifiers" | "Field.getName" |
                        "Field.get*" | "Field.set*",
                        fieldDesc, sourceMethod, sourceLine, _, _) =>
                        addField(fieldDesc, sourceMethod, entries.head, sourceLine)

                    case Array("Method.getDeclaringClass" | "Method.getModifiers" |
                        "Method.getName" | "Method.invoke",
                        methodDesc, sourceMethod, sourceLine, _, _) =>
                        addMethod(methodDesc, sourceMethod, entries.head, sourceLine)

                    case e => throw new RuntimeException(s"unexpected log entry ${e.mkString(",")}")

                }
            }

        }

        new TamiFlexLogData(classes, methods, fields)
    }

    private[this] def toJVMType(javaType: String): String = {
        val trimmedType = javaType.trim
        if (trimmedType.endsWith("[]")) "["+toJVMType(trimmedType.substring(0, trimmedType.length - 2))
        else trimmedType match {
            case "void"    => "V"
            case "byte"    => "B"
            case "char"    => "C"
            case "double"  => "D"
            case "float"   => "F"
            case "int"     => "I"
            case "long"    => "J"
            case "short"   => "S"
            case "boolean" => "Z"
            case _         => "L"+trimmedType.replace('.', '/')+";"
        }
    }

    private[this] def toDeclaredMethod(
        methodDesc: String
    )(implicit declaredMethods: DeclaredMethods): DeclaredMethod = {
        val regex = "<([^:]+): ([^ ]+) ([^(]+)\\(([^)]*)\\)>".r
        methodDesc match {
            case regex(declaringClass, returnType, name, parameterTypes) =>
                val declaringClassType = FieldType(toJVMType(declaringClass)).asObjectType
                val jvmSignature = parameterTypes.split(',').map(toJVMType).mkString("(", "", ")"+toJVMType(returnType))
                declaredMethods(declaringClassType, declaringClassType.packageName, declaringClassType, name, MethodDescriptor(jvmSignature))
        }

    }

    private[this] def toField(fieldDesc: String, project: SomeProject): Option[Field] = {
        val regex = "<([^:]+): ([^ ]+) ([^>]+)>".r
        fieldDesc match {
            case regex(declaringClass, fieldTypeDesc, name) =>
                val declaringClassType = FieldType(toJVMType(declaringClass)).asObjectType
                val fieldType = FieldType(toJVMType(fieldTypeDesc))
                project.resolveFieldReference(declaringClassType, name, fieldType)
        }
    }
}
