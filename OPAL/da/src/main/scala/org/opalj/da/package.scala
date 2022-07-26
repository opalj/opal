/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import scala.annotation.switch
import scala.xml.Node
import scala.xml.Text
import scala.xml.NodeSeq
import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContext
import org.opalj.bi.VisibilityModifier

import scala.collection.immutable.ArraySeq

/**
 * Defines convenience methods related to representing certain class file elements.
 *
 * @author Michael Eichberg
 * @author Andre Pacak
 */
package object da {

    type Constant_Pool_Index = ClassFileReader.Constant_Pool_Index
    type Constant_Pool = ClassFileReader.Constant_Pool

    type Interfaces = ArraySeq[Constant_Pool_Index]
    def NoInterfaces: Interfaces = ArraySeq.empty

    type Methods = ArraySeq[Method_Info]
    def NoMethods: Methods = ArraySeq.empty

    type MethodParameters = ArraySeq[MethodParameter]

    type Fields = ArraySeq[Field_Info]
    def NoFields: Fields = ArraySeq.empty

    type Attributes = ArraySeq[Attribute]
    def NoAttributes: Attributes = ArraySeq.empty

    type ExceptionIndexTable = ArraySeq[Constant_Pool_Index]

    type ExceptionTable = ArraySeq[ExceptionTableEntry]
    def NoExceptionTable: ExceptionTable = ArraySeq.empty

    type ElementValuePairs = ArraySeq[ElementValuePair]
    def NoElementValuePairs: ElementValuePairs = ArraySeq.empty

    type Annotations = ArraySeq[da.Annotation]
    type ParameterAnnotations = Annotations
    type ParametersAnnotations = ArraySeq[ParameterAnnotations]

    type TypeAnnotations = ArraySeq[TypeAnnotation]

    type StackMapFrames = ArraySeq[StackMapFrame]
    type TypeAnnotationPathElementsTable = ArraySeq[TypeAnnotationPathElement]

    type LocalvarTable = ArraySeq[LocalvarTableEntry]

    type VerificationTypeInfos = ArraySeq[VerificationTypeInfo]

    type PackageIndexTable = ArraySeq[Constant_Pool_Index]

    type Requires = ArraySeq[RequiresEntry]
    type Exports = ArraySeq[ExportsEntry]
    type ExportsToIndexTable = ArraySeq[Constant_Pool_Index] // Array[CONSTANT_Module_Index]
    type Opens = ArraySeq[OpensEntry]
    type OpensToIndexTable = ArraySeq[Constant_Pool_Index] // Array[CONSTANT_Module_Index]
    type Uses = ArraySeq[Constant_Pool_Index] // Array[CONSTANT_Class_Index]
    type Provides = ArraySeq[ProvidesEntry]
    type ProvidesWithIndexTable = ArraySeq[Constant_Pool_Index] // Array[CONSTANT_Class_Index]

    type ClassesArray = ArraySeq[Constant_Pool_Index] // Array[Constant_Pool_Index]

    type RecordComponents = ArraySeq[RecordComponent]

    /**
     * A node representing the context's access flags and a string that can be used
     * for filtering purposes and can be attached to the member's node.
     *
     * In the string the default visibility is represented using the name `default`.
     */
    def accessFlagsToXHTML(access_flags: Int, context: AccessFlagsContext): (NodeSeq, String) = {
        val accessFlags = AccessFlags.toString(access_flags, context)

        val explicitAccessFlags =
            VisibilityModifier.get(access_flags) match {
                case None => if (accessFlags.length() == 0) "default" else accessFlags+" default"
                case _    => accessFlags
            }

        (
            {
                if (accessFlags.isEmpty)
                    NodeSeq.Empty
                else
                    Seq(<span class="access_flags">{ accessFlags }</span>, Text(" "))
            },
            explicitAccessFlags
        )
    }

    def asJavaReferenceType(cpIndex: Int)(implicit cp: Constant_Pool): FieldTypeInfo = {
        val t = cp(cpIndex).toString(cp)
        if (t.charAt(0) == '[')
            parseFieldType(t)
        else
            asJavaObjectType(t)
    }

    def asJavaObjectType(
        cpIndex: Constant_Pool_Index
    )(
        implicit
        cp: Constant_Pool
    ): ObjectTypeInfo = {
        asJavaObjectType(cp(cpIndex).toString(cp))
    }

    def asJavaObjectType(t: String): ObjectTypeInfo = ObjectTypeInfo(t.replace('/', '.'))

    def returnTypeAsJavaType(
        type_index: Constant_Pool_Index
    )(
        implicit
        cp: Constant_Pool
    ): TypeInfo = {
        parseReturnType(cp(type_index).toString)
    }

    def parseReturnType(rt: String): TypeInfo = {
        if (rt.charAt(0) == 'V') VoidTypeInfo else parseFieldType(rt)
    }

    /**
     * Returns a string representation of the type and the information whether the (element) type
     * is a base type.
     */
    def parseFieldType(
        type_index: Constant_Pool_Index
    )(
        implicit
        cp: Constant_Pool
    ): FieldTypeInfo = {
        parseFieldType(cp(type_index).toString)
    }

    /**
     * Returns a string representation of the type and the information whether the (element) type
     * is a base type.
     */
    def parseFieldType(descriptor: String): FieldTypeInfo = {
        (descriptor.charAt(0): @scala.annotation.switch) match {
            case 'B' => ByteTypeInfo
            case 'C' => CharTypeInfo
            case 'D' => DoubleTypeInfo
            case 'F' => FloatTypeInfo
            case 'I' => IntTypeInfo
            case 'J' => LongTypeInfo
            case 'S' => ShortTypeInfo
            case 'Z' => BooleanTypeInfo
            case 'L' => asJavaObjectType(descriptor.substring(1, descriptor.length - 1))
            case '[' =>
                val componentType = descriptor.substring(1)
                parseFieldType(componentType) match {
                    case ArrayTypeInfo(elementType, dimensions, elementTypeIsBaseType) =>
                        ArrayTypeInfo(elementType, dimensions + 1, elementTypeIsBaseType)
                    case TypeInfo(elementType, elementTypeIsBaseType) =>
                        ArrayTypeInfo(elementType, 1, elementTypeIsBaseType)
                }

            case _ =>
                val message = s"$descriptor is not a valid field type descriptor"
                throw new IllegalArgumentException(message)
        }
    }

    def parseMethodDescriptor(descriptor: String): (IndexedSeq[FieldTypeInfo], TypeInfo) = {
        var index = 1 // we are not interested in the leading '('
        var parameterTypes: IndexedSeq[FieldTypeInfo] = IndexedSeq.empty
        while (descriptor.charAt(index) != ')') {
            val (ft, nextIndex) = parseParameterType(descriptor, index)
            parameterTypes = parameterTypes :+ ft
            index = nextIndex
        }

        (
            parameterTypes,
            parseReturnType(descriptor.substring(index + 1))
        )

    }

    def methodDescriptorAsInlineNode(
        methodName:       String,
        descriptor:       String,
        methodParameters: Option[MethodParameters]
    )(
        implicit
        cp: Constant_Pool
    ): Node = {
        var index = 1 // we are not interested in the leading '('
        var parameters: IndexedSeq[FieldTypeInfo] = IndexedSeq.empty
        while (descriptor.charAt(index) != ')') {
            val (fti, nextIndex) = parseParameterType(descriptor, index)
            parameters = parameters :+ fti
            index = nextIndex
        }
        val returnType = parseReturnType(descriptor.substring(index + 1)).asSpan("return")
        <span class="method_signature">
            { returnType }
            <span class="name">{ methodName }</span>
            <span class="parameters">({
                if (parameters.nonEmpty) {
                    val spanParameters: Seq[Node] =
                        if (methodParameters.isEmpty) {
                            parameters map { p => p.asSpan("parameter") }
                        } else {
                            parameters.zip(methodParameters.get) map { parameter =>
                                val (fti, methodParameter) = parameter
                                methodParameter.toXHTML(fti)
                            }
                        }
                    spanParameters.tail.foldLeft(List(spanParameters.head)) { (r, n) =>
                        r ++ List(Text(", "), n)
                    }
                } else {
                    NodeSeq.Empty
                }
            })</span>
        </span>
    }

    /**
     * Internal helper function to parse method descriptors.
     */
    private[da] def parseParameterType(md: String, startIndex: Int): (FieldTypeInfo, Int) = {
        val td = md.charAt(startIndex)
        (td: @switch) match {
            case 'L' =>
                val endIndex = md.indexOf(';', startIndex + 1)
                ( // this is the return tuple
                    ObjectTypeInfo(md.substring(startIndex + 1, endIndex).replace('/', '.')),
                    endIndex + 1
                )
            case '[' =>
                parseParameterType(md, startIndex + 1) match {
                    case (ati: ArrayTypeInfo, index) =>
                        (
                            ArrayTypeInfo(
                                ati.elementTypeAsJava,
                                ati.dimensions + 1,
                                ati.elementTypeIsBaseType
                            ),
                                index
                        )
                    case (t, index) =>
                        (ArrayTypeInfo(t.asJava, 1, t.elementTypeIsBaseType), index)
                }
            case _ =>
                (
                    parseFieldType(td.toString),
                    startIndex + 1
                )
        }
    }

    def abbreviateType(definingType: String, memberType: String): Node = {
        val classAttrtibute = "type "+(if (definingType.indexOf('[') == -1) "object" else "array")

        val abbreviatedMemberType = org.opalj.bytecode.abbreviateType(definingType, memberType, '.')
        <span class={ classAttrtibute } data-type={ memberType }> { abbreviatedMemberType } </span>
    }

    def byteArrayToNode(info: Array[Byte]): Node = {
        <pre>{ info.map(b => f"$b%02x").grouped(32).map(_.mkString("", " ", "\n")).mkString }</pre>
    }
}
