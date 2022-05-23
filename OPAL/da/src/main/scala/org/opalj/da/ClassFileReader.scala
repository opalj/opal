/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import org.opalj.bi.AttributeParent

import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

/**
 * Factory methods to read class files and create [[ClassFile]] objects.
 *
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
object ClassFileReader
    extends Constant_PoolBinding
    with bi.reader.ClassFileReader
    with bi.reader.FieldsReader
    with bi.reader.MethodsReader
    with bi.reader.AttributesReader
    with bi.reader.Unknown_attributeReader
    with bi.reader.BootstrapMethods_attributeReader
    with bi.reader.Code_attributeReader
    with bi.reader.CodeReader
    with bi.reader.SourceFile_attributeReader
    with bi.reader.Signature_attributeReader
    with bi.reader.ConstantValue_attributeReader
    with bi.reader.Synthetic_attributeReader
    with bi.reader.Deprecated_attributeReader
    with bi.reader.SourceDebugExtension_attributeReader
    with bi.reader.InnerClasses_attributeReader
    with bi.reader.Exceptions_attributeReader
    with bi.reader.EnclosingMethod_attributeReader
    with bi.reader.LineNumberTable_attributeReader
    with bi.reader.LocalVariableTable_attributeReader
    with bi.reader.LocalVariableTypeTable_attributeReader
    with bi.reader.ElementValuePairsReader
    with bi.reader.ParametersAnnotationsReader
    with bi.reader.MethodParameters_attributeReader
    with bi.reader.AnnotationsReader
    with bi.reader.AnnotationDefault_attributeReader
    with bi.reader.RuntimeVisibleAnnotations_attributeReader
    with bi.reader.RuntimeInvisibleAnnotations_attributeReader
    with bi.reader.RuntimeVisibleParameterAnnotations_attributeReader
    with bi.reader.RuntimeInvisibleParameterAnnotations_attributeReader
    with bi.reader.VerificationTypeInfoReader
    with bi.reader.StackMapTable_attributeReader
    with bi.reader.StackMapFrameReader
    with bi.reader.TypeAnnotationTargetReader
    with bi.reader.RuntimeInvisibleTypeAnnotations_attributeReader
    with bi.reader.RuntimeVisibleTypeAnnotations_attributeReader
    with bi.reader.TypeAnnotationPathReader
    with bi.reader.TypeAnnotationsReader
    // JAVA 9
    with bi.reader.Module_attributeReader
    with bi.reader.ModuleMainClass_attributeReader
    with bi.reader.ModulePackages_attributeReader
    // JAVA 11
    with bi.reader.NestHost_attributeReader
    with bi.reader.NestMembers_attributeReader
    // JAVA 16
    with bi.reader.Record_attributeReader {

    type ClassFile = da.ClassFile

    type Attribute = da.Attribute
    override implicit val attributeType: ClassTag[Attribute] = ClassTag(classOf[da.Attribute])

    type Field_Info = da.Field_Info
    override implicit val fieldInfoType: ClassTag[Field_Info] = ClassTag(classOf[da.Field_Info])

    type Method_Info = da.Method_Info
    override implicit val methodInfoType: ClassTag[Method_Info] = ClassTag(classOf[da.Method_Info])

    final override def reifyEmptyAttributes: Boolean = true

    def ClassFile(
        cp:            Constant_Pool,
        minor_version: Int,
        major_version: Int,
        access_flags:  Int,
        this_class:    Constant_Pool_Index,
        super_class:   Constant_Pool_Index,
        interfaces:    Interfaces,
        fields:        Fields,
        methods:       Methods,
        attributes:    Attributes
    ): ClassFile = {
        new ClassFile(
            cp, minor_version, major_version, access_flags,
            this_class, super_class, ArraySeq.from(interfaces),
            fields, methods, attributes
        )
    }

    def Field_Info(
        cp:               Constant_Pool,
        access_flags:     Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes
    ): Field_Info = {
        new Field_Info(access_flags, name_index, descriptor_index, attributes)
    }

    def Method_Info(
        cp:               Constant_Pool,
        access_flags:     Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes
    ): Method_Info = {
        new Method_Info(access_flags, name_index, descriptor_index, attributes)
    }

    type SourceFile_attribute = da.SourceFile_attribute
    def SourceFile_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        sourceFile_index:     Constant_Pool_Index
    ): SourceFile_attribute = {
        new SourceFile_attribute(attribute_name_index, sourceFile_index)
    }

    type Signature_attribute = da.Signature_attribute
    def Signature_attribute(
        cp:                   Constant_Pool,
        ap:                   AttributeParent,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Int,
        signature_index:      Int
    ): Signature_attribute = {
        new Signature_attribute(attribute_name_index, signature_index)
    }

    type ConstantValue_attribute = da.ConstantValue_attribute
    def ConstantValue_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Int,
        constant_value_index: Int
    ): ConstantValue_attribute = {
        new ConstantValue_attribute(attribute_name_index, constant_value_index)
    }

    type Synthetic_attribute = da.Synthetic_attribute
    def Synthetic_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index
    ): Synthetic_attribute = {
        new Synthetic_attribute(attribute_name_index)
    }

    type Deprecated_attribute = da.Deprecated_attribute
    def Deprecated_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index
    ): Deprecated_attribute = {
        new Deprecated_attribute(attribute_name_index)
    }

    type SourceDebugExtension_attribute = da.SourceDebugExtension_attribute
    def SourceDebugExtension_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        debug_extension:      Array[Byte]
    ): SourceDebugExtension_attribute = {
        new SourceDebugExtension_attribute(attribute_name_index, debug_extension)
    }

    type BootstrapMethods_attribute = da.BootstrapMethods_attribute

    type BootstrapMethod = da.BootstrapMethod
    override implicit val bootstrapMethodType: ClassTag[BootstrapMethod] = ClassTag(classOf[BootstrapMethod])

    type BootstrapArgument = da.BootstrapArgument
    override implicit val bootstrapArgumentType: ClassTag[BootstrapArgument] = ClassTag(classOf[BootstrapArgument])

    def BootstrapMethods_attribute(
        constant_pool:        Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        bootstrap_methods:    BootstrapMethods
    ): BootstrapMethods_attribute = {
        new BootstrapMethods_attribute(attribute_name_index, bootstrap_methods)
    }

    def BootstrapMethod(
        cp:         Constant_Pool,
        method_ref: Int,
        arguments:  BootstrapArguments
    ): BootstrapMethod = {
        new BootstrapMethod(method_ref, arguments)
    }

    def BootstrapArgument(cp: Constant_Pool, cp_ref: Int): BootstrapArgument = {
        new BootstrapArgument(cp_ref)
    }

    type InnerClasses_attribute = da.InnerClasses_attribute
    def InnerClasses_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        classes:              InnerClasses
    ): InnerClasses_attribute = {
        new InnerClasses_attribute(attribute_name_index, classes)
    }

    type InnerClassesEntry = da.InnerClassesEntry
    override implicit val innerClassesEntryType: ClassTag[InnerClassesEntry] = ClassTag(classOf[da.InnerClassesEntry])
    def InnerClassesEntry(
        cp:                       Constant_Pool,
        inner_class_info_index:   Constant_Pool_Index,
        outer_class_info_index:   Constant_Pool_Index,
        inner_name_index:         Constant_Pool_Index,
        inner_class_access_flags: Constant_Pool_Index
    ): InnerClassesEntry = {
        new InnerClassesEntry(
            inner_class_info_index, outer_class_info_index,
            inner_name_index,
            inner_class_access_flags
        )
    }

    type Exceptions_attribute = da.Exceptions_attribute
    def Exceptions_attribute(
        cp:                    Constant_Pool,
        ap_name_index:         Constant_Pool_Index,
        ap_descriptor_index:   Constant_Pool_Index,
        attribute_name_index:  Constant_Pool_Index,
        exception_index_table: ExceptionIndexTable
    ): Exceptions_attribute = {
        new Exceptions_attribute(attribute_name_index, ArraySeq.unsafeWrapArray(exception_index_table))
    }

    type Instructions = da.Code
    def Instructions(
        cp:                  Constant_Pool,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        instructions:        Array[Byte]
    ): Instructions = {
        new Instructions(instructions)
    }

    type ExceptionTableEntry = da.ExceptionTableEntry
    override implicit val exceptionTableEntryType: ClassTag[ExceptionTableEntry] = ClassTag(classOf[da.ExceptionTableEntry])
    def ExceptionTableEntry(
        cp:         Constant_Pool,
        start_pc:   Int,
        end_pc:     Int,
        handler_pc: Int,
        catch_type: Int
    ): ExceptionTableEntry = {
        new ExceptionTableEntry(start_pc, end_pc, handler_pc, catch_type)
    }

    type Code_attribute = da.Code_attribute
    def Code_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        max_stack:            Int,
        max_locals:           Int,
        instructions:         Instructions,
        exception_table:      ExceptionHandlers,
        attributes:           Attributes
    ): Code_attribute = {
        new Code_attribute(
            attribute_name_index,
            max_stack, max_locals,
            instructions,
            exception_table,
            attributes
        )
    }

    final override def loadsInterfacesOnly: Boolean = false

    type Unknown_attribute = da.Unknown_attribute
    def Unknown_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        info:                 Array[Byte]
    ): Unknown_attribute = {
        new Unknown_attribute(attribute_name_index, info)
    }

    type EnclosingMethod_attribute = da.EnclosingMethod_attribute
    def EnclosingMethod_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        class_index:          Constant_Pool_Index,
        method_index:         Constant_Pool_Index
    ): EnclosingMethod_attribute = {
        new EnclosingMethod_attribute(attribute_name_index, class_index, method_index)
    }

    type LineNumberTable_attribute = da.LineNumberTable_attribute
    def LineNumberTable_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        line_number_table:    LineNumbers
    ): LineNumberTable_attribute = {
        new LineNumberTable_attribute(attribute_name_index, line_number_table)
    }

    type LineNumberTableEntry = da.LineNumberTableEntry
    override implicit val lineNumberTableEntryType: ClassTag[LineNumberTableEntry] = ClassTag(classOf[da.LineNumberTableEntry])
    def LineNumberTableEntry(start_pc: Int, line_number: Int): LineNumberTableEntry = {
        new LineNumberTableEntry(start_pc, line_number)
    }

    type LocalVariableTable_attribute = da.LocalVariableTable_attribute
    def LocalVariableTable_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        local_variable_table: LocalVariables
    ): LocalVariableTable_attribute = {
        new LocalVariableTable_attribute(attribute_name_index, local_variable_table)
    }

    type LocalVariableTableEntry = da.LocalVariableTableEntry
    override implicit val localVariableTableEntryType: ClassTag[LocalVariableTableEntry] = ClassTag(classOf[da.LocalVariableTableEntry])
    def LocalVariableTableEntry(
        cp:               Constant_Pool,
        start_pc:         Int,
        length:           Int,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        index:            Int
    ): LocalVariableTableEntry = {
        new LocalVariableTableEntry(start_pc, length, name_index, descriptor_index, index)
    }

    type LocalVariableTypeTable_attribute = da.LocalVariableTypeTable_attribute
    def LocalVariableTypeTable_attribute(
        cp:                        Constant_Pool,
        ap_name_index:             Constant_Pool_Index,
        ap_descriptor_index:       Constant_Pool_Index,
        attribute_name_index:      Constant_Pool_Index,
        local_variable_type_table: LocalVariableTypes
    ): LocalVariableTypeTable_attribute = {
        new LocalVariableTypeTable_attribute(
            attribute_name_index, local_variable_type_table
        )
    }

    type LocalVariableTypeTableEntry = da.LocalVariableTypeTableEntry
    override implicit val localVariableTypeTableEntryType: ClassTag[LocalVariableTypeTableEntry] = ClassTag(classOf[da.LocalVariableTypeTableEntry])
    def LocalVariableTypeTableEntry(
        cp:              Constant_Pool,
        start_pc:        Int,
        length:          Int,
        name_index:      Constant_Pool_Index,
        signature_index: Constant_Pool_Index,
        index:           Int
    ): LocalVariableTypeTableEntry = {
        new LocalVariableTypeTableEntry(
            start_pc, length, name_index, signature_index, index
        )
    }

    type ElementValuePair = da.ElementValuePair
    override implicit val elementValuePairType: ClassTag[ElementValuePair] = ClassTag(classOf[da.ElementValuePair])

    type ElementValue = da.ElementValue
    override implicit val elementValueType: ClassTag[ElementValue] = ClassTag(classOf[da.ElementValue])
    def ElementValuePair(
        cp:                 Constant_Pool,
        element_name_index: Constant_Pool_Index, element_value: ElementValue
    ): ElementValuePair = {
        new ElementValuePair(element_name_index, element_value)
    }

    def ByteValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        new ByteValue(const_value_index)
    }

    def CharValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        new CharValue(const_value_index)
    }

    def DoubleValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        new DoubleValue(const_value_index)
    }

    def FloatValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        new FloatValue(const_value_index)
    }

    def IntValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        new IntValue(const_value_index)
    }

    def LongValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        new LongValue(const_value_index)
    }

    def ShortValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        new ShortValue(const_value_index)
    }

    def BooleanValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        new BooleanValue(const_value_index)
    }

    def StringValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        new StringValue(const_value_index)
    }

    def ClassValue(cp: Constant_Pool, const_value_index: Constant_Pool_Index): ElementValue = {
        new ClassValue(const_value_index)
    }

    def EnumValue(
        cp:               Constant_Pool,
        type_name_index:  Constant_Pool_Index,
        const_name_index: Constant_Pool_Index
    ): ElementValue = {
        new EnumValue(type_name_index, const_name_index)
    }

    type Annotation = da.Annotation
    override implicit val annotationType: ClassTag[Annotation] = ClassTag(classOf[da.Annotation])
    def AnnotationValue(cp: Constant_Pool, annotation: Annotation): ElementValue = {
        new AnnotationValue(annotation)
    }

    def ArrayValue(cp: Constant_Pool, values: ElementValues): ElementValue = new ArrayValue(values)

    def Annotation(
        cp:                  Constant_Pool,
        type_index:          Constant_Pool_Index,
        element_value_pairs: ElementValuePairs
    ): Annotation = {
        new Annotation(type_index, element_value_pairs)
    }

    type AnnotationDefault_attribute = da.AnnotationDefault_attribute
    def AnnotationDefault_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        element_value:        ElementValue
    ): AnnotationDefault_attribute = {
        new AnnotationDefault_attribute(attribute_name_index, element_value)
    }

    type RuntimeVisibleAnnotations_attribute = da.RuntimeVisibleAnnotations_attribute
    def RuntimeVisibleAnnotations_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        annotations:          Annotations
    ): RuntimeVisibleAnnotations_attribute = {
        new RuntimeVisibleAnnotations_attribute(attribute_name_index, annotations)
    }

    type RuntimeInvisibleAnnotations_attribute = da.RuntimeInvisibleAnnotations_attribute
    def RuntimeInvisibleAnnotations_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        annotations:          Annotations
    ): RuntimeInvisibleAnnotations_attribute = {
        new RuntimeInvisibleAnnotations_attribute(attribute_name_index, annotations)
    }

    type RuntimeVisibleParameterAnnotations_attribute = da.RuntimeVisibleParameterAnnotations_attribute
    def RuntimeVisibleParameterAnnotations_attribute(
        cp:                     Constant_Pool,
        ap_name_index:          Constant_Pool_Index,
        ap_descriptor_index:    Constant_Pool_Index,
        attribute_name_index:   Constant_Pool_Index,
        parameters_annotations: ParametersAnnotations
    ): RuntimeVisibleParameterAnnotations_attribute = {
        new RuntimeVisibleParameterAnnotations_attribute(
            attribute_name_index, parameters_annotations
        )
    }

    type RuntimeInvisibleParameterAnnotations_attribute = da.RuntimeInvisibleParameterAnnotations_attribute
    def RuntimeInvisibleParameterAnnotations_attribute(
        cp:                     Constant_Pool,
        ap_name_index:          Constant_Pool_Index,
        ap_descriptor_index:    Constant_Pool_Index,
        attribute_name_index:   Constant_Pool_Index,
        parameters_annotations: ParametersAnnotations
    ): RuntimeInvisibleParameterAnnotations_attribute = {
        new RuntimeInvisibleParameterAnnotations_attribute(
            attribute_name_index, parameters_annotations
        )
    }

    type StackMapFrame = da.StackMapFrame
    override implicit val stackMapFrameType: ClassTag[StackMapFrame] = ClassTag(classOf[da.StackMapFrame])

    type StackMapTable_attribute = da.StackMapTable_attribute
    def StackMapTable_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        stack_map_frames:     StackMapFrames
    ): StackMapTable_attribute = {
        new StackMapTable_attribute(attribute_name_index, stack_map_frames)
    }

    def SameFrame(frame_type: Int): StackMapFrame = new SameFrame(frame_type)

    type VerificationTypeInfo = da.VerificationTypeInfo
    override implicit val verificationTypeInfoType: ClassTag[VerificationTypeInfo] = ClassTag(classOf[da.VerificationTypeInfo])

    def SameLocals1StackItemFrame(
        frame_type:                   Int,
        verification_type_info_stack: VerificationTypeInfo
    ): StackMapFrame = {
        new SameLocals1StackItemFrame(frame_type, verification_type_info_stack)
    }

    def ChopFrame(frame_type: Int, offset_delta: Int): StackMapFrame = {
        new ChopFrame(frame_type, offset_delta)
    }

    def SameFrameExtended(frame_type: Int, offset_delta: Int): StackMapFrame = {
        new SameFrameExtended(frame_type, offset_delta)
    }

    def AppendFrame(
        frame_type:                    Int,
        offset_delta:                  Int,
        verification_type_info_locals: VerificationTypeInfoLocals
    ): StackMapFrame = {
        new AppendFrame(frame_type, offset_delta, verification_type_info_locals)
    }

    def SameLocals1StackItemFrameExtended(
        frame_type:                   Int,
        offset_delta:                 Int,
        verification_type_info_stack: VerificationTypeInfo
    ): StackMapFrame = {
        new SameLocals1StackItemFrameExtended(
            frame_type, offset_delta, verification_type_info_stack
        )
    }

    def FullFrame(
        frame_type:                    Int,
        offset_delta:                  Int,
        verification_type_info_locals: VerificationTypeInfoLocals,
        verification_type_info_stack:  VerificationTypeInfoStack
    ): StackMapFrame = {
        new FullFrame(
            frame_type, offset_delta, verification_type_info_locals, verification_type_info_stack
        )
    }

    type TopVariableInfo = da.TopVariableInfo.type
    def TopVariableInfo(): VerificationTypeInfo = da.TopVariableInfo

    type IntegerVariableInfo = da.IntegerVariableInfo.type
    def IntegerVariableInfo(): VerificationTypeInfo = da.IntegerVariableInfo

    type FloatVariableInfo = da.FloatVariableInfo.type
    def FloatVariableInfo(): VerificationTypeInfo = da.FloatVariableInfo

    type LongVariableInfo = da.LongVariableInfo.type
    def LongVariableInfo(): VerificationTypeInfo = da.LongVariableInfo

    type DoubleVariableInfo = da.DoubleVariableInfo.type
    def DoubleVariableInfo(): VerificationTypeInfo = da.DoubleVariableInfo

    type NullVariableInfo = da.NullVariableInfo.type
    def NullVariableInfo(): VerificationTypeInfo = da.NullVariableInfo

    type UninitializedThisVariableInfo = da.UninitializedThisVariableInfo.type
    def UninitializedThisVariableInfo(): VerificationTypeInfo = da.UninitializedThisVariableInfo

    type UninitializedVariableInfo = da.UninitializedVariableInfo
    def UninitializedVariableInfo(offset: Int): VerificationTypeInfo = {
        new UninitializedVariableInfo(offset)
    }

    type ObjectVariableInfo = da.ObjectVariableInfo
    def ObjectVariableInfo(cp: Constant_Pool, cpool_index: Int): VerificationTypeInfo = {
        new ObjectVariableInfo(cpool_index)
    }

    type MethodParameters_attribute = da.MethodParameters_attribute
    def MethodParameters_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        parameters:           MethodParameters
    ): MethodParameters_attribute = {
        new MethodParameters_attribute(attribute_name_index, parameters)
    }

    type MethodParameter = da.MethodParameter
    override implicit val methodParameterType: ClassTag[MethodParameter] = ClassTag(classOf[da.MethodParameter])
    def MethodParameter(
        cp:           Constant_Pool,
        name_index:   Constant_Pool_Index,
        access_flags: Int
    ): MethodParameter = {
        new MethodParameter(name_index, access_flags)
    }

    type TypeAnnotationTarget = da.TypeAnnotationTarget
    def ParameterDeclarationOfClassOrInterface(
        type_parameter_index: Int
    ): TypeAnnotationTarget = {
        TATParameterDeclarationOfClassOrInterface(type_parameter_index)
    }

    def ParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Constant_Pool_Index
    ): TypeAnnotationTarget = {
        TATParameterDeclarationOfMethodOrConstructor(type_parameter_index)
    }

    def SupertypeTarget(supertype_index: Int): TypeAnnotationTarget = {
        TATSupertype(supertype_index)
    }

    def TypeBoundOfParameterDeclarationOfClassOrInterface(
        type_parameter_index: Constant_Pool_Index,
        bound_index:          Constant_Pool_Index
    ): TypeAnnotationTarget = {
        TATTypeBoundOfParameterDeclarationOfClassOrInterface(type_parameter_index, bound_index)
    }

    def TypeBoundOfParameterDeclarationOfMethodOrConstructor(
        type_parameter_index: Constant_Pool_Index,
        bound_index:          Constant_Pool_Index
    ): TypeAnnotationTarget = {
        TATTypeBoundOfParameterDeclarationOfMethodOrConstructor(type_parameter_index, bound_index)
    }

    def FieldDeclaration: TypeAnnotationTarget = da.TATFieldDeclaration

    def ReturnType: TypeAnnotationTarget = da.TATReturnType

    def ReceiverType: TypeAnnotationTarget = da.TATReceiverType

    def FormalParameter(formal_parameter_index: Int): TypeAnnotationTarget = {
        TATFormalParameter(formal_parameter_index)
    }

    def Throws(throws_type_index: Constant_Pool_Index): TypeAnnotationTarget = {
        TATThrows(throws_type_index)
    }

    def Catch(exception_table_index: Int): TypeAnnotationTarget = {
        TATCatch(exception_table_index)
    }

    type LocalvarTableEntry = da.LocalvarTableEntry
    override implicit val localvarTableEntryType: ClassTag[LocalvarTableEntry] = ClassTag(classOf[da.LocalvarTableEntry])
    def LocalvarTableEntry(
        start_pc:                   Int,
        length:                     Int,
        local_variable_table_index: Int
    ): LocalvarTableEntry = {
        new LocalvarTableEntry(start_pc, length, local_variable_table_index)
    }

    def LocalvarDecl(localVarTable: LocalvarTable): TypeAnnotationTarget = {
        TATLocalvarDecl(localVarTable)
    }

    def ResourcevarDecl(localVarTable: LocalvarTable): TypeAnnotationTarget = {
        TATResourcevarDecl(localVarTable)
    }

    def InstanceOf(offset: Int): TypeAnnotationTarget = TATInstanceOf(offset)

    def New(offset: Int): TypeAnnotationTarget = TATNew(offset)

    def MethodReferenceExpressionNew /*::New*/ (offset: Int): TypeAnnotationTarget = {
        TATMethodReferenceExpressionNew(offset)
    }

    def MethodReferenceExpressionIdentifier /*::Identifier*/ (offset: Int): TypeAnnotationTarget = {
        TATMethodReferenceExpressionIdentifier(offset)
    }

    def CastExpression(offset: Int, type_argument_index: Int): TypeAnnotationTarget = {
        TATCastExpression(offset, type_argument_index)
    }

    def ConstructorInvocation(offset: Int, type_argument_index: Int): TypeAnnotationTarget = {
        TATConstructorInvocation(offset, type_argument_index)
    }

    def MethodInvocation(offset: Int, type_argument_index: Int): TypeAnnotationTarget = {
        TATMethodInvocation(offset, type_argument_index)
    }

    def ConstructorInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
    ): TypeAnnotationTarget = {
        TATConstructorInMethodReferenceExpression(offset, type_argument_index)
    }

    def MethodInMethodReferenceExpression(
        offset:              Int,
        type_argument_index: Int
    ): TypeAnnotationTarget = {
        TATMethodInMethodReferenceExpression(offset, type_argument_index)
    }

    type TypeAnnotationPath = da.TypeAnnotationPath
    def TypeAnnotationDirectlyOnType: da.TypeAnnotationDirectlyOnType.type = da.TypeAnnotationDirectlyOnType

    type TypeAnnotationPathElement = da.TypeAnnotationPathElement
    override implicit val typeAnnotationPathElementType: ClassTag[TypeAnnotationPathElement] = ClassTag(classOf[da.TypeAnnotationPathElement])
    def TypeAnnotationPath(path: TypeAnnotationPathElementsTable): TypeAnnotationPath = {
        TypeAnnotationPathElements(path)
    }

    /**
     * The `type_path_kind` was `0` (and the type_argument_index was also `0`).
     */
    def TypeAnnotationDeeperInArrayType: da.TypeAnnotationDeeperInArrayType.type = {
        da.TypeAnnotationDeeperInArrayType
    }

    /**
     * The `type_path_kind` was `1` (and the type_argument_index was (as defined by the
     * specification) also `0`).
     */
    def TypeAnnotationDeeperInNestedType: da.TypeAnnotationDeeperInNestedType.type = {
        da.TypeAnnotationDeeperInNestedType
    }

    /**
     * The `type_path_kind` was `2` (and the type_argument_index was (as defined by the
     * specification) also `0`).
     */
    def TypeAnnotationOnBoundOfWildcardType: da.TypeAnnotationOnBoundOfWildcardType.type = {
        da.TypeAnnotationOnBoundOfWildcardType
    }

    def TypeAnnotationOnTypeArgument(type_argument_index: Int): TypeAnnotationPathElement = {
        new TypeAnnotationOnTypeArgument(type_argument_index)
    }

    type TypeAnnotation = da.TypeAnnotation
    override implicit val typeAnnotationType: ClassTag[TypeAnnotation] = ClassTag(classOf[da.TypeAnnotation])
    def TypeAnnotation(
        cp:                  Constant_Pool,
        target:              TypeAnnotationTarget,
        path:                TypeAnnotationPath,
        type_index:          Constant_Pool_Index,
        element_value_pairs: ElementValuePairs
    ): TypeAnnotation = {
        new TypeAnnotation(target, path, type_index, element_value_pairs)
    }

    type RuntimeInvisibleTypeAnnotations_attribute = da.RuntimeInvisibleTypeAnnotations_attribute
    def RuntimeInvisibleTypeAnnotations_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        annotations:          TypeAnnotations
    ): RuntimeInvisibleTypeAnnotations_attribute = {
        new RuntimeInvisibleTypeAnnotations_attribute(attribute_name_index, annotations)
    }

    type RuntimeVisibleTypeAnnotations_attribute = da.RuntimeVisibleTypeAnnotations_attribute
    def RuntimeVisibleTypeAnnotations_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        annotations:          TypeAnnotations
    ): RuntimeVisibleTypeAnnotations_attribute = {
        new RuntimeVisibleTypeAnnotations_attribute(attribute_name_index, annotations)
    }

    // --------------------------------------------------------------------------------------------
    // JAVA 9
    // --------------------------------------------------------------------------------------------

    type Module_attribute = da.Module_attribute

    type RequiresEntry = da.RequiresEntry
    override implicit val requiresEntryType: ClassTag[RequiresEntry] = ClassTag(classOf[da.RequiresEntry])

    type ExportsEntry = da.ExportsEntry
    override implicit val exportsEntryType: ClassTag[ExportsEntry] = ClassTag(classOf[da.ExportsEntry])

    type OpensEntry = da.OpensEntry
    override implicit val opensEntryType: ClassTag[OpensEntry] = ClassTag(classOf[da.OpensEntry])

    type ProvidesEntry = da.ProvidesEntry
    override implicit val providesEntryType: ClassTag[ProvidesEntry] = ClassTag(classOf[da.ProvidesEntry])

    def Module_attribute(
        constant_pool:        Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        module_name_index:    Constant_Pool_Index, // CONSTANT_Module_info
        module_flags:         Int,
        module_version_index: Constant_Pool_Index, // CONSTANT_UTF8
        requires:             Requires,
        exports:              Exports,
        opens:                Opens,
        uses:                 Uses,
        provides:             Provides
    ): Module_attribute = {
        new Module_attribute(
            attribute_name_index,
            module_name_index, module_flags, module_version_index,
            requires, exports, opens, ArraySeq.from(uses), provides
        )
    }

    def RequiresEntry(
        constant_pool:         Constant_Pool,
        requires_index:        Constant_Pool_Index,
        requires_flags:        Int,
        require_version_index: Constant_Pool_Index
    ): RequiresEntry = {
        new RequiresEntry(requires_index, requires_flags, require_version_index)
    }

    def ExportsEntry(
        constant_pool:          Constant_Pool,
        exports_index:          Constant_Pool_Index,
        exports_flags:          Int,
        exports_to_index_table: ExportsToIndexTable
    ): ExportsEntry = {
        new ExportsEntry(
            exports_index,
            exports_flags,
            ArraySeq.from(exports_to_index_table)
        )
    }

    def OpensEntry(
        constant_pool:        Constant_Pool,
        opens_index:          Constant_Pool_Index,
        opens_flags:          Int,
        opens_to_index_table: OpensToIndexTable
    ): OpensEntry = {
        new OpensEntry(opens_index, opens_flags, ArraySeq.from(opens_to_index_table))
    }

    def ProvidesEntry(
        constant_pool:             Constant_Pool,
        provides_index:            Constant_Pool_Index, // CONSTANT_Class
        provides_with_index_table: ProvidesWithIndexTable
    ): ProvidesEntry = {
        new ProvidesEntry(provides_index, ArraySeq.from(provides_with_index_table))
    }

    type ModulePackages_attribute = da.ModulePackages_attribute
    def ModulePackages_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        package_index_table:  PackageIndexTable
    ): ModulePackages_attribute = {
        new ModulePackages_attribute(
            attribute_name_index, ArraySeq.from(package_index_table)
        )
    }

    type ModuleMainClass_attribute = da.ModuleMainClass_attribute
    def ModuleMainClass_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        main_class_index:     Constant_Pool_Index
    ): ModuleMainClass_attribute = {
        new ModuleMainClass_attribute(attribute_name_index, main_class_index)
    }

    // --------------------------------------------------------------------------------------------
    // JAVA 11
    // --------------------------------------------------------------------------------------------

    type NestHost_attribute = da.NestHost_attribute
    def NestHost_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        host_class_index:     Constant_Pool_Index
    ): NestHost_attribute = {
        new NestHost_attribute(attribute_name_index, host_class_index)
    }

    type NestMembers_attribute = da.NestMembers_attribute
    def NestMembers_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        classes_array:        ClassesArray
    ): NestMembers_attribute = {
        new NestMembers_attribute(
            attribute_name_index, ArraySeq.from(classes_array)
        )
    }

    // --------------------------------------------------------------------------------------------
    // JAVA 16
    // --------------------------------------------------------------------------------------------

    type Record_attribute = da.Record_attribute
    def Record_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        components:           RecordComponents
    ): Record_attribute = {
        new Record_attribute(attribute_name_index, components)
    }

    type RecordComponent = da.RecordComponent
    override implicit val recordComponentType: ClassTag[RecordComponent] = ClassTag(classOf[da.RecordComponent])
    def RecordComponent(
        cp:               Constant_Pool,
        name_index:       Constant_Pool_Index,
        descriptor_index: Constant_Pool_Index,
        attributes:       Attributes
    ): RecordComponent = {
        new RecordComponent(name_index, descriptor_index, attributes)
    }

}
