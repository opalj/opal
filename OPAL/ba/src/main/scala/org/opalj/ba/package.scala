/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj

import scala.language.implicitConversions
import scala.annotation.switch
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import org.opalj.collection.immutable.UShortPair
import org.opalj.collection.immutable.IntIntPair
import org.opalj.log.GlobalLogContext
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.bi.ACC_PUBLIC
import org.opalj.bi.ACC_FINAL
import org.opalj.bi.ACC_SUPER
import org.opalj.bi.ACC_INTERFACE
import org.opalj.bi.ACC_ABSTRACT
import org.opalj.bi.ACC_ENUM
import org.opalj.bi.ACC_ANNOTATION
import org.opalj.bi.ACC_STATIC
import org.opalj.bi.ACC_PROTECTED
import org.opalj.bi.ACC_SYNCHRONIZED
import org.opalj.bi.ACC_BRIDGE
import org.opalj.bi.ACC_SYNTHETIC
import org.opalj.bi.ACC_PRIVATE
import org.opalj.bi.ACC_VARARGS
import org.opalj.bi.ACC_TRANSIENT
import org.opalj.bi.ACC_VOLATILE
import org.opalj.bi.ACC_NATIVE
import org.opalj.bi.ACC_STRICT
import org.opalj.bi.ACC_OPEN
import org.opalj.bi.ACC_MODULE
import org.opalj.bi.ACC_TRANSITIVE
import org.opalj.bi.ACC_MANDATED
import org.opalj.bi.ACC_STATIC_PHASE
import org.opalj.bi.ConstantPoolTags.CONSTANT_Class_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Fieldref_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Methodref_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_InterfaceMethodref_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_String_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Integer_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Float_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Long_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Double_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_NameAndType_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Utf8_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_MethodHandle_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_MethodType_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_InvokeDynamic_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Module_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Package_ID
import org.opalj.bi.ConstantPoolTags.CONSTANT_Dynamic_ID
import org.opalj.br.Attribute
import org.opalj.br.Code
import org.opalj.br.ObjectType
import org.opalj.br.cp._
import org.opalj.br.instructions._

import scala.collection.immutable.ArraySeq

/**
 * Implementation of an eDSL for creating Java bytecode. The eDSL is designed to facilitate
 * the creation of correct class files; i.e., whenever possible it tries to fill wholes. For
 * example, when an interface is specified the library automatically ensures that the super
 * class type is (initially) set to `java.lang.Object` as required by the JVM specification.
 *
 * This package in particular provides functionality to convert [[org.opalj.br]] classes to
 * [[org.opalj.da]] classes.
 *
 * @author Michael Eichberg
 * @author Malte Limmeroth
 */
package object ba { ba =>

    final val FrameworkName = "OPAL Bytecode Assembler"

    {
        // Log the information whether a production build or a development build is used.
        implicit val logContext: LogContext = GlobalLogContext
        import OPALLogger.info
        try {
            scala.Predef.assert(false)
            // when we reach this point assertions are off...
            info(FrameworkName, s"Production Build")
        } catch {
            case _: AssertionError => info(FrameworkName, "Development Build with Assertions")
        }
    }

    implicit def methodAttributeBuilderToSeq(
        b: br.MethodAttributeBuilder
    ): Seq[br.MethodAttributeBuilder] = {
        Seq(b)
    }

    implicit def codeAttributeBuilderToSome[T](
        b: br.CodeAttributeBuilder[T]
    ): Some[br.CodeAttributeBuilder[T]] = {
        Some(b)
    }

    implicit def attributeToMethodAttributeBuilder(a: br.Attribute): br.MethodAttributeBuilder = {
        new br.MethodAttributeBuilder {
            def apply(
                accessFlags: Int,
                name:        String,
                descriptor:  br.MethodDescriptor
            ): Attribute = {
                a
            }
        }
    }

    implicit def attributeToFieldAttributeBuilder(a: br.Attribute): br.FieldAttributeBuilder = {
        new br.FieldAttributeBuilder {
            def apply(accessFlags: Int, name: String, fieldType: br.FieldType): Attribute = a
        }
    }

    implicit def attributeToClassFileAttributeBuilder(
        a: br.Attribute
    ): br.ClassFileAttributeBuilder = {
        new br.ClassFileAttributeBuilder {
            def apply(
                version:        UShortPair,
                accessFlags:    Int,
                thisType:       ObjectType,
                superclassType: Option[ObjectType],
                interfaceTypes: ArraySeq[ObjectType], // TODO Use a UIDSet here ...
                fields:         ArraySeq[br.FieldTemplate],
                methods:        ArraySeq[br.MethodTemplate]
            ): Attribute = {
                a
            }
        }
    }

    final val PUBLIC = new AccessModifier(ACC_PUBLIC.mask)
    final val FINAL = new AccessModifier(ACC_FINAL.mask)
    final val SUPER = new AccessModifier(ACC_SUPER.mask)
    final val INTERFACE = new AccessModifier(ACC_INTERFACE.mask)
    final val ABSTRACT = new AccessModifier(ACC_ABSTRACT.mask)
    final val SYNTHETIC = new AccessModifier(ACC_SYNTHETIC.mask)
    final val ANNOTATION = new AccessModifier(ACC_ANNOTATION.mask)
    final val ENUM = new AccessModifier(ACC_ENUM.mask)
    final val PRIVATE = new AccessModifier(ACC_PRIVATE.mask)
    final val PROTECTED = new AccessModifier(ACC_PROTECTED.mask)
    final val STATIC = new AccessModifier(ACC_STATIC.mask)
    final val SYNCHRONIZED = new AccessModifier(ACC_SYNCHRONIZED.mask)
    final val BRIDGE = new AccessModifier(ACC_BRIDGE.mask)
    final val VARARGS = new AccessModifier(ACC_VARARGS.mask)
    final val NATIVE = new AccessModifier(ACC_NATIVE.mask)
    final val STRICT = new AccessModifier(ACC_STRICT.mask)
    final val VOLATILE = new AccessModifier(ACC_VOLATILE.mask)
    final val TRANSIENT = new AccessModifier(ACC_TRANSIENT.mask)
    final val MODULE = new AccessModifier(ACC_MODULE.mask)
    final val OPEN = new AccessModifier(ACC_OPEN.mask)
    final val MANDATED = new AccessModifier(ACC_MANDATED.mask)
    final val TRANSITIVE = new AccessModifier(ACC_TRANSITIVE.mask)
    final val STATIC_PHASE = new AccessModifier(ACC_STATIC_PHASE.mask)

    // *********************************************************************************************
    //
    //          F U N C T I O N A L I T Y   T O   C R E A T E   "br."   C L A S S F I L E S
    //
    // *********************************************************************************************

    def createBoostrapMethodTableAttribute(constantsPool: ConstantsPool): da.Attribute = {
        import constantsPool._
        val bootstrap_methods = bootstrapMethods map { bootstrapMethod =>
            new da.BootstrapMethod(
                CPEMethodHandle(bootstrapMethod.handle, false),
                bootstrapMethod.arguments.map[da.BootstrapArgument] { argument =>
                    new da.BootstrapArgument(CPEntryForBootstrapArgument(argument))
                }
            )
        }
        val attributeNameIndex = constantsPool.CPEUtf8(bi.BootstrapMethodsAttribute.Name)
        new da.BootstrapMethods_attribute(attributeNameIndex, bootstrap_methods)
    }

    /**
     * Converts a [[org.opalj.br.ClassFile]] to a [[org.opalj.da.ClassFile]] and all its attributes
     * to the attributes in [[org.opalj.da]].
     */
    def toDA(
        classFile: br.ClassFile
    )(
        implicit
        toDAConfig: ToDAConfig = ToDAConfig.RetainAllAttributes
    ): da.ClassFile = {
        implicit val constantsBuffer = ConstantsBuffer(ConstantsBuffer.collectLDCs(classFile))
        val thisTypeCPRef = constantsBuffer.CPEClass(classFile.thisType, false)
        val superClassCPRef = classFile.superclassType match {
            case Some(superclassType) => constantsBuffer.CPEClass(superclassType, false)
            case None                 => 0
        }

        val interfaces = classFile.interfaceTypes.map(i => constantsBuffer.CPEClass(i, false))
        val fields = classFile.fields.map[da.Field_Info](toDA)
        val methods = classFile.methods.map[da.Method_Info](toDA)
        var attributes = classFile.attributes.flatMap[da.Attribute](a => toDA(a))
        val (constantPoolEntries, constantsPool) = constantsBuffer.build
        if (constantsPool.bootstrapMethods.nonEmpty) {
            attributes :+= createBoostrapMethodTableAttribute(constantsPool)
        }
        val constant_pool = constantPoolEntries.toDA
        da.ClassFile(
            constant_pool = constant_pool,
            minor_version = classFile.version.minor,
            major_version = classFile.version.major,
            access_flags = classFile.accessFlags,
            this_class = thisTypeCPRef,
            super_class = superClassCPRef,
            interfaces = interfaces,
            fields = fields,
            methods = methods,
            attributes = attributes
        )
    }

    def toDA(
        field: br.Field
    )(
        implicit
        constantsBuffer: ConstantsBuffer,
        config:          ToDAConfig
    ): da.Field_Info = {
        da.Field_Info(
            access_flags = field.accessFlags,
            name_index = constantsBuffer.CPEUtf8(field.name),
            descriptor_index = constantsBuffer.CPEUtf8(field.fieldType.toJVMTypeName),
            attributes = field.attributes.flatMap[da.Attribute](a => toDA(a))
        )
    }

    def toDA(
        method: br.Method
    )(
        implicit
        constantsBuffer: ConstantsBuffer,
        config:          ToDAConfig
    ): da.Method_Info = {
        var attributes: da.Attributes = method.attributes.flatMap[da.Attribute](a => toDA(a))
        if (method.body.isDefined) {
            attributes = toDA(method.body.get) +: attributes
        }
        da.Method_Info(
            access_flags = method.accessFlags,
            name_index = constantsBuffer.CPEUtf8(method.name),
            descriptor_index = constantsBuffer.CPEUtf8(method.descriptor.toJVMDescriptor),
            attributes = attributes
        )
    }

    def toDA(
        code: Code
    )(
        implicit
        constantsBuffer: ConstantsBuffer,
        config:          ToDAConfig
    ): da.Code_attribute = {
        import constantsBuffer._
        val data = new ByteArrayOutputStream(code.instructions.length)
        val instructions = new DataOutputStream(data)

        def writeMethodRef(i: Instruction): MethodInvocationInstruction = {
            val mi @ MethodInvocationInstruction(declaringClass, isInterface, name, descriptor) = i
            val cpeRef =
                if (isInterface)
                    CPEInterfaceMethodRef(declaringClass, name, descriptor)
                else
                    CPEMethodRef(declaringClass, name, descriptor)
            instructions.writeShort(cpeRef)
            mi
        }

        var modifiedByWide = false
        code iterate { (pc, i) =>
            val opcode = i.opcode
            instructions.writeByte(opcode)

            (i.opcode: @switch) match {

                case ALOAD_0.opcode | ALOAD_1.opcode | ALOAD_2.opcode | ALOAD_3.opcode |
                    ASTORE_0.opcode | ASTORE_1.opcode | ASTORE_2.opcode | ASTORE_3.opcode |
                    ILOAD_0.opcode | ILOAD_1.opcode | ILOAD_2.opcode | ILOAD_3.opcode |
                    ISTORE_0.opcode | ISTORE_1.opcode | ISTORE_2.opcode | ISTORE_3.opcode |
                    DLOAD_0.opcode | DLOAD_1.opcode | DLOAD_2.opcode | DLOAD_3.opcode |
                    DSTORE_0.opcode | DSTORE_1.opcode | DSTORE_2.opcode | DSTORE_3.opcode |
                    FLOAD_0.opcode | FLOAD_1.opcode | FLOAD_2.opcode | FLOAD_3.opcode |
                    FSTORE_0.opcode | FSTORE_1.opcode | FSTORE_2.opcode | FSTORE_3.opcode |
                    LLOAD_0.opcode | LLOAD_1.opcode | LLOAD_2.opcode | LLOAD_3.opcode |
                    LSTORE_0.opcode | LSTORE_1.opcode | LSTORE_2.opcode | LSTORE_3.opcode |

                    IRETURN.opcode | LRETURN.opcode | FRETURN.opcode | DRETURN.opcode |
                    ARETURN.opcode |
                    RETURN.opcode |

                    ARRAYLENGTH.opcode |
                    AASTORE.opcode |
                    IASTORE.opcode | SASTORE.opcode | BASTORE.opcode | CASTORE.opcode |
                    FASTORE.opcode |
                    LASTORE.opcode |
                    DASTORE.opcode |
                    AALOAD.opcode |
                    DALOAD.opcode | FALOAD.opcode |
                    IALOAD.opcode | LALOAD.opcode | SALOAD.opcode | BALOAD.opcode | CALOAD.opcode |

                    DADD.opcode | FADD.opcode | IADD.opcode | LADD.opcode |
                    DDIV.opcode | FDIV.opcode | IDIV.opcode | LDIV.opcode |
                    DNEG.opcode | FNEG.opcode | INEG.opcode | LNEG.opcode |
                    DMUL.opcode | FMUL.opcode | IMUL.opcode | LMUL.opcode |
                    DREM.opcode | FREM.opcode | IREM.opcode | LREM.opcode |
                    DSUB.opcode | FSUB.opcode | ISUB.opcode | LSUB.opcode |

                    IAND.opcode | LAND.opcode |
                    IOR.opcode | LOR.opcode |
                    IXOR.opcode | LXOR.opcode |
                    ISHL.opcode | LSHL.opcode |
                    ISHR.opcode | LSHR.opcode |
                    IUSHR.opcode | LUSHR.opcode |

                    ICONST_0.opcode | ICONST_1.opcode |
                    ICONST_2.opcode | ICONST_3.opcode |
                    ICONST_4.opcode | ICONST_5.opcode |
                    ICONST_M1.opcode |
                    ACONST_NULL.opcode |
                    DCONST_0.opcode | DCONST_1.opcode |
                    FCONST_0.opcode | FCONST_1.opcode | FCONST_2.opcode |
                    LCONST_0.opcode | LCONST_1.opcode |
                    DCMPG.opcode | FCMPG.opcode | DCMPL.opcode | FCMPL.opcode |
                    LCMP.opcode |

                    NOP.opcode |
                    POP.opcode | POP2.opcode |
                    SWAP.opcode |
                    DUP.opcode | DUP_X1.opcode | DUP_X2.opcode |
                    DUP2.opcode | DUP2_X1.opcode | DUP2_X2.opcode |
                    D2F.opcode | I2F.opcode | L2F.opcode |
                    D2I.opcode | F2I.opcode | L2I.opcode |
                    D2L.opcode | I2L.opcode | F2L.opcode |
                    F2D.opcode | I2D.opcode | L2D.opcode |
                    I2C.opcode | I2B.opcode | I2S.opcode |

                    MONITORENTER.opcode | MONITOREXIT.opcode |

                    ATHROW.opcode =>
                // Nothing to do; the opcode is already written!

                case ALOAD.opcode | ASTORE.opcode |
                    ILOAD.opcode | ISTORE.opcode |
                    DLOAD.opcode | DSTORE.opcode |
                    FLOAD.opcode | FSTORE.opcode |
                    LLOAD.opcode | LSTORE.opcode =>
                    val ExplicitLocalVariableIndex(index) = i
                    if (modifiedByWide) {
                        modifiedByWide = false
                        instructions.writeShort(index)
                    } else {
                        instructions.writeByte(index)
                    }

                case BIPUSH.opcode =>
                    val BIPUSH(value) = i
                    instructions.writeByte(value)

                case SIPUSH.opcode =>
                    val SIPUSH(value) = i
                    instructions.writeShort(value)

                case NEW.opcode =>
                    val NEW(objectType) = i
                    instructions.writeShort(CPEClass(objectType, false))

                case CHECKCAST.opcode =>
                    val CHECKCAST(referenceType) = i
                    val cpeRef = CPEClass(referenceType, false)
                    instructions.writeShort(cpeRef)

                case INSTANCEOF.opcode =>
                    val INSTANCEOF(referenceType) = i
                    instructions.writeShort(CPEClass(referenceType, false))

                case IINC.opcode =>
                    val IINC(lvIndex, constValue) = i
                    if (modifiedByWide) {
                        modifiedByWide = false
                        instructions.writeShort(lvIndex)
                        instructions.writeShort(constValue)
                    } else {
                        instructions.writeByte(lvIndex)
                        instructions.writeByte(constValue)
                    }

                case JSR.opcode =>
                    val JSR(branchoffset) = i
                    instructions.writeShort(branchoffset)
                case JSR_W.opcode =>
                    val JSR_W(branchoffset) = i
                    instructions.writeInt(branchoffset)
                case RET.opcode =>
                    val RET(lvIndex) = i
                    instructions.writeByte(lvIndex)

                case GOTO.opcode =>
                    val GOTO(branchoffset) = i
                    instructions.writeShort(branchoffset)
                case GOTO_W.opcode =>
                    val GOTO_W(branchoffset) = i
                    instructions.writeInt(branchoffset)

                case IF_ICMPEQ.opcode | IF_ICMPNE.opcode |
                    IF_ICMPLT.opcode | IF_ICMPLE.opcode |
                    IF_ICMPGT.opcode | IF_ICMPGE.opcode |
                    IFEQ.opcode | IFNE.opcode |
                    IFLT.opcode | IFLE.opcode |
                    IFGT.opcode | IFGE.opcode |
                    IF_ACMPEQ.opcode | IF_ACMPNE.opcode |
                    IFNONNULL.opcode | IFNULL.opcode =>
                    val SimpleConditionalBranchInstruction(branchoffset) = i
                    instructions.writeShort(branchoffset)

                case PUTSTATIC.opcode | PUTFIELD.opcode | GETSTATIC.opcode | GETFIELD.opcode =>
                    val FieldAccess(declaringClass, fieldName, fieldType) = i
                    val jvmFieldType = fieldType.toJVMTypeName
                    val cpeRef = CPEFieldRef(declaringClass, fieldName, jvmFieldType)
                    instructions.writeShort(cpeRef)

                case INVOKESPECIAL.opcode | INVOKEVIRTUAL.opcode | INVOKESTATIC.opcode =>
                    writeMethodRef(i)

                case INVOKEINTERFACE.opcode =>
                    val invokeinterface = writeMethodRef(i)
                    instructions.writeByte(invokeinterface.count)
                    instructions.writeByte(0)

                case NEWARRAY.opcode =>
                    instructions.writeByte(i.asInstanceOf[NEWARRAY].atype)

                case ANEWARRAY.opcode =>
                    val ANEWARRAY(referenceType) = i
                    instructions.writeShort(CPEClass(referenceType, false))

                case MULTIANEWARRAY.opcode =>
                    val MULTIANEWARRAY(arrayType, dimensions) = i
                    instructions.writeShort(CPEClass(arrayType, false))
                    instructions.writeByte(dimensions)

                case LDC.opcode =>
                    val cpIndex = ConstantsBuffer.getOrCreateCPEntry(i.asInstanceOf[LDC[_]])
                    instructions.writeByte(cpIndex)

                case LDC_W.opcode =>
                    instructions.writeShort(
                        i match {
                            case LoadInt_W(value)          => CPEInteger(value, false)
                            case LoadFloat_W(value)        => CPEFloat(value, false)
                            case LoadClass_W(value)        => CPEClass(value, false)
                            case LoadString_W(value)       => CPEString(value, false)

                            case LoadMethodHandle_W(value) => CPEMethodHandle(value, false)
                            case LoadMethodType_W(value)   => CPEMethodType(value, false)

                            case LoadDynamic_W(bootstrapMethod, name, descriptor) =>
                                CPEDynamic(bootstrapMethod, name, descriptor, false)
                            case INCOMPLETE_LDC_W =>
                                throw ConstantPoolException("incomplete LDC_W")
                        }
                    )

                case LDC2_W.opcode =>
                    i match {
                        case LoadLong(value)   => instructions.writeShort(CPELong(value))
                        case LoadDouble(value) => instructions.writeShort(CPEDouble(value))

                        case LoadDynamic2_W(bootstrapMethod, name, descriptor) =>
                            instructions.writeShort(
                                CPEDynamic(bootstrapMethod, name, descriptor, false)
                            )
                        case INCOMPLETE_LDC2_W => throw ConstantPoolException("incomplete LDC2_W")
                    }

                case INVOKEDYNAMIC.opcode =>
                    val INVOKEDYNAMIC(bootstrapMethod, name, descriptor) = i
                    val cpEntryIndex = CPEInvokeDynamic(bootstrapMethod, name, descriptor)
                    // CPEInvokeDynamic automatically creates all cp entries required when we
                    // later on transform the bootstrap method
                    instructions.writeShort(cpEntryIndex)
                    instructions.writeByte(0)
                    instructions.writeByte(0)

                case TABLESWITCH.opcode =>
                    val TABLESWITCH(defaultOffset, low, high, jumpOffsets) = i
                    var padding = 3 - (pc % 4)
                    while (padding > 0) { instructions.writeByte(0); padding -= 1 }
                    instructions.writeInt(defaultOffset)
                    instructions.writeInt(low)
                    instructions.writeInt(high)
                    jumpOffsets.foreach { instructions.writeInt }

                case LOOKUPSWITCH.opcode =>
                    val LOOKUPSWITCH(defaultOffset, npairs) = i
                    var padding = 3 - (pc % 4)
                    while (padding > 0) { instructions.writeByte(0); padding -= 1 }
                    instructions.writeInt(defaultOffset)
                    instructions.writeInt(npairs.size)
                    npairs.foreach { pair =>
                        val IntIntPair(matchValue, offset) = pair
                        instructions.writeInt(matchValue)
                        instructions.writeInt(offset)
                    }

                case WIDE.opcode =>
                    if (modifiedByWide) throw new IllegalArgumentException(s"$pc: wide after wide")
                    // modifiedByWide will be set to false by the subsequent instruction
                    modifiedByWide = true
            }
        }

        instructions.flush

        da.Code_attribute(
            attribute_name_index = constantsBuffer.CPEUtf8(bi.CodeAttribute.Name),
            max_stack = code.maxStack,
            max_locals = code.maxLocals,
            code = da.Code(data.toByteArray),
            exceptionTable = code.exceptionHandlers.map[da.ExceptionTableEntry](toDA),
            attributes = code.attributes.flatMap[da.Attribute](a => toDA(a))
        )
    }

    def toDA(
        exceptionHandler: br.ExceptionHandler
    )(
        implicit
        constantsBuffer: ConstantsBuffer
    ): da.ExceptionTableEntry = {
        val index = if (exceptionHandler.catchType.isDefined) {
            constantsBuffer.CPEClass(exceptionHandler.catchType.get, false)
        } else 0
        da.ExceptionTableEntry(
            exceptionHandler.startPC,
            exceptionHandler.endPC,
            exceptionHandler.handlerPC,
            index
        )
    }

    def toDA(
        elementValue: br.ElementValue
    )(
        implicit
        constantsBuffer: ConstantsBuffer,
        config:          ToDAConfig
    ): da.ElementValue = {
        import constantsBuffer._
        (elementValue.kindId: @switch) match {
            case br.ByteValue.KindId =>
                val br.ByteValue(value) = elementValue
                da.ByteValue(CPEInteger(value.toInt, false))

            case br.CharValue.KindId =>
                val br.CharValue(value) = elementValue
                da.CharValue(CPEInteger(value.toInt, false))

            case br.DoubleValue.KindId =>
                val br.DoubleValue(value) = elementValue
                da.DoubleValue(CPEDouble(value))

            case br.FloatValue.KindId =>
                val br.FloatValue(value) = elementValue
                da.FloatValue(CPEFloat(value, false))

            case br.IntValue.KindId =>
                val br.IntValue(value) = elementValue
                da.IntValue(CPEInteger(value, false))

            case br.LongValue.KindId =>
                val br.LongValue(value) = elementValue
                da.LongValue(CPELong(value))

            case br.ShortValue.KindId =>
                val br.ShortValue(value) = elementValue
                da.ShortValue(CPEInteger(value.toInt, false))

            case br.BooleanValue.KindId =>
                val br.BooleanValue(value) = elementValue
                da.BooleanValue(CPEInteger(if (value) 1 else 0, false))

            case br.StringValue.KindId =>
                val br.StringValue(value) = elementValue
                da.StringValue(CPEUtf8(value))

            case br.ClassValue.KindId =>
                val br.ClassValue(value) = elementValue
                da.ClassValue(CPEUtf8(value.toJVMTypeName))

            case br.EnumValue.KindId =>
                val br.EnumValue(enumType, enumName) = elementValue
                da.EnumValue(CPEUtf8(enumType.toJVMTypeName), CPEUtf8(enumName))

            case br.ArrayValue.KindId =>
                val br.ArrayValue(values) = elementValue
                val daElementValues = values.map { ev: br.ElementValue => toDA(ev) }
                da.ArrayValue(daElementValues)

            case br.AnnotationValue.KindId =>
                val br.AnnotationValue(annotation) = elementValue
                da.AnnotationValue(toDA(annotation))
        }
    }

    def toDA(
        annotation: br.Annotation
    )(
        implicit
        constantsBuffer: ConstantsBuffer,
        config:          ToDAConfig
    ): da.Annotation = {
        val br.Annotation(t, evps) = annotation
        val daEVPs = evps.map[da.ElementValuePair] { evp =>
            da.ElementValuePair(constantsBuffer.CPEUtf8(evp.name), toDA(evp.value))
        }
        da.Annotation(constantsBuffer.CPEUtf8(t.toJVMTypeName), daEVPs)
    }

    def toDA(
        localvarTableEntry: br.LocalvarTableEntry
    ): da.LocalvarTableEntry = {
        val br.LocalvarTableEntry(startPC, length, index) = localvarTableEntry
        da.LocalvarTableEntry(startPC, length, index)
    }

    def toDA(
        typeAnnotationTarget: br.TypeAnnotationTarget
    ): da.TypeAnnotationTarget = {
        (typeAnnotationTarget.typeId: @switch) match {

            case 0x00 =>
                val br.TAOfParameterDeclarationOfClassOrInterface(index) = typeAnnotationTarget
                da.TATParameterDeclarationOfClassOrInterface(index)
            case 0x01 =>
                val br.TAOfParameterDeclarationOfMethodOrConstructor(index) = typeAnnotationTarget
                da.TATParameterDeclarationOfMethodOrConstructor(index)
            case 0x10 =>
                val br.TAOfSupertype(index) = typeAnnotationTarget
                da.TATSupertype(index)

            case 0x11 =>
                val br.TAOfTypeBoundOfParameterDeclarationOfClassOrInterface(
                    typeIndex,
                    boundIndex
                    ) = typeAnnotationTarget
                da.TATTypeBoundOfParameterDeclarationOfClassOrInterface(typeIndex, boundIndex)
            case 0x12 =>
                val br.TAOfTypeBoundOfParameterDeclarationOfMethodOrConstructor(
                    typeIndex,
                    boundIndex
                    ) = typeAnnotationTarget
                da.TATTypeBoundOfParameterDeclarationOfMethodOrConstructor(typeIndex, boundIndex)

            case 0x13 => da.TATFieldDeclaration
            case 0x14 => da.TATReturnType
            case 0x15 => da.TATReceiverType

            case 0x16 =>
                val br.TAOfFormalParameter(index) = typeAnnotationTarget
                da.TATFormalParameter(index)

            case 0x17 =>
                val br.TAOfThrows(index) = typeAnnotationTarget
                da.TATThrows(index)

            case 0x40 =>
                val br.TAOfLocalvarDecl(lvtes) = typeAnnotationTarget
                da.TATLocalvarDecl(lvtes.map[da.LocalvarTableEntry](toDA))
            case 0x41 =>
                val br.TAOfResourcevarDecl(lvtes) = typeAnnotationTarget
                da.TATResourcevarDecl(lvtes.map[da.LocalvarTableEntry](toDA))

            case 0x42 =>
                val br.TAOfCatch(index) = typeAnnotationTarget
                da.TATCatch(index)

            case 0x43 =>
                val br.TAOfInstanceOf(offset) = typeAnnotationTarget
                da.TATInstanceOf(offset)

            case 0x44 =>
                val br.TAOfNew(offset) = typeAnnotationTarget
                da.TATNew(offset)

            case 0x45 =>
                val br.TAOfMethodReferenceExpressionNew(offset) = typeAnnotationTarget
                da.TATMethodReferenceExpressionNew(offset)

            case 0x46 =>
                val br.TAOfMethodReferenceExpressionIdentifier(offset) = typeAnnotationTarget
                da.TATMethodReferenceExpressionIdentifier(offset)

            case 0x47 =>
                val br.TAOfCastExpression(offset, index) = typeAnnotationTarget
                da.TATCastExpression(offset, index)

            case 0x48 =>
                val br.TAOfConstructorInvocation(offset, index) = typeAnnotationTarget
                da.TATConstructorInvocation(offset, index)

            case 0x49 =>
                val br.TAOfMethodInvocation(offset, index) = typeAnnotationTarget
                da.TATMethodInvocation(offset, index)

            case 0x4A =>
                val br.TAOfConstructorInMethodReferenceExpression(offset, index) =
                    typeAnnotationTarget
                da.TATConstructorInMethodReferenceExpression(offset, index)

            case 0x4B =>
                val br.TAOfMethodInMethodReferenceExpression(offset, index) = typeAnnotationTarget
                da.TATMethodInMethodReferenceExpression(offset, index)
        }
    }

    def toDA(
        typeAnnotationPathElement: br.TypeAnnotationPathElement
    ): da.TypeAnnotationPathElement = {
        (typeAnnotationPathElement.kindId: @switch) match {
            case br.TADeeperInArrayType.KindId     => da.TypeAnnotationDeeperInArrayType
            case br.TADeeperInNestedType.KindId    => da.TypeAnnotationDeeperInNestedType
            case br.TAOnBoundOfWildcardType.KindId => da.TypeAnnotationOnBoundOfWildcardType
            case br.TAOnTypeArgument.KindId =>
                val br.TAOnTypeArgument(index) = typeAnnotationPathElement
                da.TypeAnnotationOnTypeArgument(index)
        }
    }

    def toDA(
        typeAnnotationPath: br.TypeAnnotationPath
    ): da.TypeAnnotationPath = {
        typeAnnotationPath match {
            case br.TADirectlyOnType     => da.TypeAnnotationDirectlyOnType
            case br.TAOnNestedType(path) => da.TypeAnnotationPathElements(path.map[da.TypeAnnotationPathElement](toDA))
        }
    }

    def toDA(
        typeAnnotation: br.TypeAnnotation
    )(
        implicit
        constantsBuffer: ConstantsBuffer,
        config:          ToDAConfig
    ): da.TypeAnnotation = {
        da.TypeAnnotation(
            toDA(typeAnnotation.target),
            toDA(typeAnnotation.path),
            constantsBuffer.CPEUtf8(typeAnnotation.annotationType.toJVMTypeName),
            typeAnnotation.elementValuePairs.map[da.ElementValuePair] { evp =>
                da.ElementValuePair(constantsBuffer.CPEUtf8(evp.name), toDA(evp.value))
            }
        )
    }

    /**
     * Converts the given [[org.opalj.br.Attribute]] to a [[org.opalj.da.Attribute]] using
     * the given configuration.
     * @see [[org.opalj.br.Attribute#kindId]] for the list of all supported attributes.
     */
    def toDA(
        attribute: Attribute
    )(
        implicit
        constantsBuffer: ConstantsBuffer,
        config:          ToDAConfig
    ): Option[da.Attribute] = {
        import constantsBuffer._
        (attribute.kindId: @switch) match {
            case br.Code.KindId => toDA(attribute.asInstanceOf[br.Code])

            // direct conversions
            case br.SourceFile.KindId =>
                val br.SourceFile(s) = attribute
                Some(da.SourceFile_attribute(CPEUtf8(bi.SourceFileAttribute.Name), CPEUtf8(s)))

            case br.Deprecated.KindId =>
                Some(da.Deprecated_attribute(CPEUtf8(bi.DeprecatedAttribute.Name)))

            case br.Synthetic.KindId =>
                Some(da.Synthetic_attribute(CPEUtf8(bi.SyntheticAttribute.Name)))

            case br.SourceDebugExtension.KindId =>
                val br.SourceDebugExtension(data) = attribute
                val attributeName = bi.SourceDebugExtensionAttribute.Name
                Some(da.SourceDebugExtension_attribute(CPEUtf8(attributeName), data))

            case br.EnclosingMethod.KindId =>
                val br.EnclosingMethod(classType, nameOption, descriptorOption) = attribute
                val classIndex = CPEClass(classType, false)
                val nameAndTypeIndex = nameOption match {
                    case Some(name) => CPENameAndType(name, descriptorOption.get.toJVMDescriptor)
                    case None       => 0
                }
                val attributeNameIndex = CPEUtf8(bi.EnclosingMethodAttribute.Name)
                Some(da.EnclosingMethod_attribute(attributeNameIndex, classIndex, nameAndTypeIndex))

            // ALL CONSTANT FIELD VALUES
            case br.ConstantFloat.KindId =>
                val br.ConstantFloat(value) = attribute
                val attributeNameIndex = CPEUtf8(bi.ConstantValueAttribute.Name)
                Some(da.ConstantValue_attribute(attributeNameIndex, CPEFloat(value, false)))
            case br.ConstantInteger.KindId =>
                val br.ConstantInteger(value) = attribute
                val attributeNameIndex = CPEUtf8(bi.ConstantValueAttribute.Name)
                Some(da.ConstantValue_attribute(attributeNameIndex, CPEInteger(value, false)))
            case br.ConstantString.KindId =>
                val br.ConstantString(value) = attribute
                val attributeNameIndex = CPEUtf8(bi.ConstantValueAttribute.Name)
                Some(da.ConstantValue_attribute(attributeNameIndex, CPEString(value, false)))
            case br.ConstantDouble.KindId =>
                val br.ConstantDouble(value) = attribute
                val attributeNameIndex = CPEUtf8(bi.ConstantValueAttribute.Name)
                Some(da.ConstantValue_attribute(attributeNameIndex, CPEDouble(value)))
            case br.ConstantLong.KindId =>
                val br.ConstantLong(value) = attribute
                val attributeNameIndex = CPEUtf8(bi.ConstantValueAttribute.Name)
                Some(da.ConstantValue_attribute(attributeNameIndex, CPELong(value)))

            //code attribute conversions
            case br.LineNumberTable.KindId =>
                val attributeNameIndex = CPEUtf8(bi.LineNumberTableAttribute.Name)
                Some(
                    da.LineNumberTable_attribute(
                        attributeNameIndex,
                        attribute match {

                            case br.UnpackedLineNumberTable(lineNumbers) =>
                                lineNumbers.map[da.LineNumberTableEntry] { l =>
                                    da.LineNumberTableEntry(l.startPC, l.lineNumber)
                                }

                            case c @ br.CompactLineNumberTable(rawLNs: Array[Byte]) =>
                                val lntBuilder = List.newBuilder[da.LineNumberTableEntry]
                                var e = 0
                                val entries = rawLNs.length / 4
                                while (e < entries) {
                                    val index = e * 4
                                    val startPC = c.asUnsignedShort(rawLNs(index), rawLNs(index + 1))
                                    val lineNumber = c.asUnsignedShort(rawLNs(index + 2), rawLNs(index + 3))
                                    lntBuilder += da.LineNumberTableEntry(startPC, lineNumber)
                                    e += 1
                                }
                                lntBuilder.result()

                            case _ =>
                                val attributeName = attribute.getClass.getName
                                val m = s"unsupported line number attribute: $attributeName"
                                throw new Error(m)
                        }
                    )
                )

            case br.LocalVariableTable.KindId =>
                val br.LocalVariableTable(localVariables) = attribute
                Some(
                    da.LocalVariableTable_attribute(
                        CPEUtf8(bi.LocalVariableTableAttribute.Name),
                        localVariables.map[da.LocalVariableTableEntry] { l =>
                            da.LocalVariableTableEntry(
                                start_pc = l.startPC,
                                length = l.length,
                                name_index = CPEUtf8(l.name),
                                descriptor_index = CPEUtf8(l.fieldType.toJVMTypeName),
                                index = l.index
                            )
                        }
                    )
                )

            case br.LocalVariableTypeTable.KindId =>
                val br.LocalVariableTypeTable(localVariableTypes) = attribute
                Some(
                    da.LocalVariableTypeTable_attribute(
                        CPEUtf8(bi.LocalVariableTypeTableAttribute.Name),
                        localVariableTypes.map[da.LocalVariableTypeTableEntry] { l =>
                            da.LocalVariableTypeTableEntry(
                                start_pc = l.startPC,
                                length = l.length,
                                name_index = CPEUtf8(l.name),
                                signature_index = CPEUtf8(l.signature.toJVMSignature),
                                index = l.index
                            )
                        }
                    )
                )

            case br.MethodParameterTable.KindId =>
                val br.MethodParameterTable(parameters) = attribute
                Some(
                    da.MethodParameters_attribute(
                        CPEUtf8(bi.MethodParametersAttribute.Name),
                        parameters.map[da.MethodParameter] { p =>
                            da.MethodParameter(
                                if (p.name.isDefined) CPEUtf8(p.name.get) else 0,
                                p.accessFlags
                            )
                        }
                    )
                )

            case br.ExceptionTable.KindId =>
                val br.ExceptionTable(exceptions) = attribute
                Some(
                    da.Exceptions_attribute(
                        CPEUtf8(bi.ExceptionsAttribute.Name),
                        exceptions.map(CPEClass(_, false))
                    )
                )

            case br.InnerClassTable.KindId =>
                val br.InnerClassTable(innerClasses) = attribute
                Some(
                    da.InnerClasses_attribute(
                        CPEUtf8(bi.InnerClassesAttribute.Name),
                        innerClasses.map[da.InnerClassesEntry] { ic =>
                            da.InnerClassesEntry(
                                CPEClass(ic.innerClassType, false),
                                ic.outerClassType.map(CPEClass(_, false)).getOrElse(0),
                                if (ic.innerName.isDefined) CPEUtf8(ic.innerName.get) else 0,
                                ic.innerClassAccessFlags
                            )
                        }
                    )
                )

            case br.StackMapTable.KindId =>
                val br.StackMapTable(brFrames) = attribute

                implicit def toDA(vti: br.VerificationTypeInfo): da.VerificationTypeInfo = {
                    (vti.tag: @switch) match {
                        case 0 => da.TopVariableInfo
                        case 1 => da.IntegerVariableInfo
                        case 2 => da.FloatVariableInfo
                        case 3 => da.DoubleVariableInfo
                        case 4 => da.LongVariableInfo
                        case 5 => da.NullVariableInfo
                        case 6 => da.UninitializedThisVariableInfo
                        case 7 =>
                            val br.ObjectVariableInfo(referenceType) = vti
                            da.ObjectVariableInfo(CPEClass(referenceType, false))
                        case 8 =>
                            val br.UninitializedVariableInfo(offset) = vti
                            da.UninitializedVariableInfo(offset)
                    }
                }

                val daFrames = brFrames.map[da.StackMapFrame] { f =>
                    val frameType = f.frameType
                    if (frameType < 64) {
                        da.SameFrame(frameType)
                    } else if (frameType < 128) {
                        val br.SameLocals1StackItemFrame(_, vti) = f
                        da.SameLocals1StackItemFrame(frameType, toDA(vti))
                    } else if (frameType < 247) {
                        throw new Error(s"unexpected/unsupported stack map frame type: $frameType")
                    } else if (frameType == 247) {
                        val br.SameLocals1StackItemFrameExtended(offsetDelta, vti) = f
                        da.SameLocals1StackItemFrameExtended(frameType, offsetDelta, toDA(vti))
                    } else if (frameType < 251) {
                        val br.ChopFrame(frameType, offsetDelta) = f
                        da.ChopFrame(frameType, offsetDelta)
                    } else if (frameType == 251) {
                        val br.SameFrameExtended(offsetDelta) = f
                        da.SameFrameExtended(frameType, offsetDelta)
                    } else if (frameType < 255) {
                        val br.AppendFrame(_, offsetDelta, vtis) = f
                        da.AppendFrame(frameType, offsetDelta, vtis.map[da.VerificationTypeInfo](toDA))
                    } else if (frameType == 255) {
                        val br.FullFrame(offsetDelta, vtiLocals, vtiStack) = f
                        da.FullFrame(255, offsetDelta, vtiLocals.map[da.VerificationTypeInfo](toDA), vtiStack.map[da.VerificationTypeInfo](toDA))
                    } else {
                        throw new Error(s"frame type out of range[0..255] $frameType")
                    }
                }
                Some(
                    da.StackMapTable_attribute(CPEUtf8(bi.StackMapTableAttribute.Name), daFrames)
                )

            /* 12-16 The Signature Attribute */
            case br.ClassSignature.KindId |
                br.MethodTypeSignature.KindId |
                br.ClassTypeSignature.KindId |
                br.ArrayTypeSignature.KindId |
                br.TypeVariableSignature.KindId =>
                val br.Signature(jvmSignature) = attribute
                val attributeNameIndex = CPEUtf8(bi.SignatureAttribute.Name)

                Some(da.Signature_attribute(attributeNameIndex, CPEUtf8(jvmSignature)))

            /* 29-41 The AnnotationDefault Attribute */
            case br.ByteValue.KindId |
                br.CharValue.KindId |
                br.DoubleValue.KindId |
                br.FloatValue.KindId |
                br.IntValue.KindId |
                br.LongValue.KindId |
                br.ShortValue.KindId |
                br.BooleanValue.KindId |
                br.StringValue.KindId |
                br.ClassValue.KindId |
                br.EnumValue.KindId |
                br.ArrayValue.KindId |
                br.AnnotationValue.KindId =>
                val ev = attribute.asInstanceOf[br.ElementValue]
                val attributeNameIndex = CPEUtf8(bi.AnnotationDefaultAttribute.Name)
                val elementValue = toDA(ev: br.ElementValue)
                Some(da.AnnotationDefault_attribute(attributeNameIndex, elementValue))

            case br.RuntimeVisibleAnnotationTable.KindId =>
                val br.RuntimeVisibleAnnotationTable(annotations) = attribute
                val attributeNameIndex = CPEUtf8(bi.RuntimeVisibleAnnotationsAttribute.Name)
                val daAnnotations = annotations.map[da.Annotation](toDA)
                Some(da.RuntimeVisibleAnnotations_attribute(attributeNameIndex, daAnnotations))

            case br.RuntimeInvisibleAnnotationTable.KindId =>
                val br.RuntimeInvisibleAnnotationTable(annotations) = attribute
                val attributeNameIndex = CPEUtf8(bi.RuntimeInvisibleAnnotationsAttribute.Name)
                val daAnnotations = annotations.map[da.Annotation](toDA)
                Some(da.RuntimeInvisibleAnnotations_attribute(attributeNameIndex, daAnnotations))

            case br.RuntimeVisibleParameterAnnotationTable.KindId =>
                val br.RuntimeVisibleParameterAnnotationTable(parameterAnnotations) = attribute
                val attributeName = bi.RuntimeVisibleParameterAnnotationsAttribute.Name
                val attributeNameIndex = CPEUtf8(attributeName)
                val daPAs = parameterAnnotations.map[da.ParameterAnnotations](as => as.map[da.Annotation](toDA))
                Some(da.RuntimeVisibleParameterAnnotations_attribute(attributeNameIndex, daPAs))

            case br.RuntimeInvisibleParameterAnnotationTable.KindId =>
                val br.RuntimeInvisibleParameterAnnotationTable(parameterAnnotations) = attribute
                val attributeName = bi.RuntimeInvisibleParameterAnnotationsAttribute.Name
                val attributeNameIndex = CPEUtf8(attributeName)
                val daPAs = parameterAnnotations.map[da.ParameterAnnotations](as => as.map[da.Annotation](toDA))
                Some(da.RuntimeInvisibleParameterAnnotations_attribute(attributeNameIndex, daPAs))

            case br.RuntimeInvisibleTypeAnnotationTable.KindId =>
                val br.RuntimeInvisibleTypeAnnotationTable(typeAnnotations) = attribute
                val attributeName = bi.RuntimeInvisibleTypeAnnotationsAttribute.Name
                val attributeNameIndex = CPEUtf8(attributeName)
                val daPAs = typeAnnotations.map[da.TypeAnnotation](toDA)
                Some(da.RuntimeInvisibleTypeAnnotations_attribute(attributeNameIndex, daPAs))

            case br.RuntimeVisibleTypeAnnotationTable.KindId =>
                val br.RuntimeVisibleTypeAnnotationTable(typeAnnotations) = attribute
                val attributeName = bi.RuntimeVisibleTypeAnnotationsAttribute.Name
                val attributeNameIndex = CPEUtf8(attributeName)
                val daPAs = typeAnnotations.map[da.TypeAnnotation](toDA)
                Some(da.RuntimeVisibleTypeAnnotations_attribute(attributeNameIndex, daPAs))

            case br.ModuleMainClass.KindId =>
                val br.ModuleMainClass(mainClassType /*:ObjectType*/ ) = attribute
                val attributeNameIndex = CPEUtf8(bi.ModuleMainClassAttribute.Name)
                val mainClassIndex = CPEClass(mainClassType, false)
                Some(da.ModuleMainClass_attribute(attributeNameIndex, mainClassIndex))

            case br.ModulePackages.KindId =>
                val br.ModulePackages(packages /*:IndexedSeq[String]*/ ) = attribute
                val attributeNameIndex = CPEUtf8(bi.ModulePackagesAttribute.Name)
                Some(da.ModulePackages_attribute(attributeNameIndex, packages.map(CPEPackage _)))

            case br.Module.KindId =>
                val br.Module(name, flags, version, requires, exports, opens, uses, provides) = attribute
                val attributeNameIndex = CPEUtf8(bi.ModuleAttribute.Name)
                Some(da.Module_attribute(
                    attributeNameIndex,
                    CPEModule(name),
                    flags,
                    version.map(CPEUtf8).getOrElse(0),
                    requires.map[da.RequiresEntry](require =>
                        da.RequiresEntry(
                            CPEModule(require.requires),
                            require.flags,
                            require.version.map(CPEUtf8).getOrElse(0)
                        )),
                    exports.map[da.ExportsEntry](export =>
                        da.ExportsEntry(
                            CPEPackage(export.exports),
                            export.flags,
                            export.exportsTo.map(CPEModule _)
                        )),
                    opens.map[da.OpensEntry](open =>
                        da.OpensEntry(
                            CPEPackage(open.opens),
                            open.flags,
                            open.toPackages.map(CPEModule _)
                        )),
                    uses.map(use => CPEClass(use, false)),
                    provides.map[da.ProvidesEntry](provide =>
                        da.ProvidesEntry(
                            CPEClass(provide.provides, false),
                            provide.withInterfaces.map(withInterface =>
                                CPEClass(withInterface, false)): ArraySeq[Int]
                        ))
                ))

            case br.NestHost.KindId =>
                val br.NestHost(hostClassType /*:ObjectType*/ ) = attribute
                val attributeNameIndex = CPEUtf8(bi.NestHostAttribute.Name)
                val hostClassIndex = CPEClass(hostClassType, false)
                Some(da.NestHost_attribute(attributeNameIndex, hostClassIndex))

            case br.NestMembers.KindId =>
                val br.NestMembers(classes /*:IndexedSeq[ObjectType]*/ ) = attribute
                val attributeNameIndex = CPEUtf8(bi.NestMembersAttribute.Name)
                val classIndices = classes.map(CPEClass(_, false))
                Some(da.NestMembers_attribute(attributeNameIndex, classIndices))

            case br.Record.KindId =>
                val br.Record(components) = attribute
                val attributeNameIndex = CPEUtf8(bi.RecordAttribute.Name)
                Some(da.Record_attribute(
                    attributeNameIndex,
                    components.map[da.RecordComponent] { c =>
                        da.RecordComponent(
                            CPEUtf8(c.name),
                            CPEUtf8(c.componentType.toJVMTypeName),
                            c.attributes.map { a => toDA(a).get }
                        )
                    }
                ))

            case br.PermittedSubclasses.KindId =>
                val br.PermittedSubclasses(subclasses) = attribute
                val attributeNameIndex = CPEUtf8(bi.PermittedSubclassesAttribute.Name)
                val cpSubclassIndices = subclasses.map(CPEClass(_, false))
                Some(da.PermittedSubclasses_attribute(
                    attributeNameIndex,
                    cpSubclassIndices
                ))
            //
            // OPAL'S OWN ATTRIBUTES
            //

            case br.VirtualTypeFlag.KindId =>
                if (config.retainOPALAttributes) {
                    val attributeNameIndex = CPEUtf8(br.VirtualTypeFlag.Name)
                    // We "hijack" the unknown attribute for our purposes
                    Some(da.Unknown_attribute(attributeNameIndex, Array()))
                } else {
                    None
                }

            case br.SynthesizedClassFiles.KindId =>
                if (config.retainOPALAttributes) {
                    ???
                } else {
                    None
                }

            case br.UnknownAttribute.KindId =>
                if (config.retainUnknownAttributes) {
                    val br.UnknownAttribute(attributeName, info) = attribute
                    val attributeNameIndex = CPEUtf8(attributeName)
                    Some(da.Unknown_attribute(attributeNameIndex, info))
                } else {
                    None
                }

            case _ =>
                throw new Error(s"unsupported attribute: ${attribute.getClass.getName}")
        }
    }

    def toDA(constantPool: Array[Constant_Pool_Entry]): Array[da.Constant_Pool_Entry] = {
        constantPool map { cpEntry =>
            if (cpEntry eq null)
                null
            else {
                (cpEntry.tag: @switch) match {
                    case CONSTANT_Class_ID =>
                        val CONSTANT_Class_info(nameIndex) = cpEntry
                        da.CONSTANT_Class_info(nameIndex)

                    case CONSTANT_Fieldref_ID =>
                        val CONSTANT_Fieldref_info(classIndex, nameAndTypeIndex) = cpEntry
                        da.CONSTANT_Fieldref_info(classIndex, nameAndTypeIndex)

                    case CONSTANT_Methodref_ID =>
                        val CONSTANT_Methodref_info(classIndex, nameAndTypeIndex) = cpEntry
                        da.CONSTANT_Methodref_info(classIndex, nameAndTypeIndex)

                    case CONSTANT_InterfaceMethodref_ID =>
                        val CONSTANT_InterfaceMethodref_info(classIndex, nameAndTypeIndex) = cpEntry
                        da.CONSTANT_InterfaceMethodref_info(classIndex, nameAndTypeIndex)

                    case CONSTANT_String_ID =>
                        val CONSTANT_String_info(s) = cpEntry
                        da.CONSTANT_String_info(s)

                    case CONSTANT_Integer_ID =>
                        val CONSTANT_Integer_info(i) = cpEntry
                        da.CONSTANT_Integer_info(i.value)

                    case CONSTANT_Float_ID =>
                        val CONSTANT_Float_info(f) = cpEntry
                        da.CONSTANT_Float_info(f.value)

                    case CONSTANT_Long_ID =>
                        val CONSTANT_Long_info(l) = cpEntry
                        da.CONSTANT_Long_info(l.value)

                    case CONSTANT_Double_ID =>
                        val CONSTANT_Double_info(d) = cpEntry
                        da.CONSTANT_Double_info(d.value)

                    case CONSTANT_NameAndType_ID =>
                        val CONSTANT_NameAndType_info(nameIndex, descriptorIndex) = cpEntry
                        da.CONSTANT_NameAndType_info(nameIndex, descriptorIndex)

                    case CONSTANT_Utf8_ID =>
                        val CONSTANT_Utf8_info(u) = cpEntry
                        da.CONSTANT_Utf8(u)

                    case CONSTANT_MethodHandle_ID =>
                        val CONSTANT_MethodHandle_info(referenceKind, referenceIndex) = cpEntry
                        da.CONSTANT_MethodHandle_info(referenceKind, referenceIndex)

                    case CONSTANT_MethodType_ID =>
                        val CONSTANT_MethodType_info(index) = cpEntry
                        da.CONSTANT_MethodType_info(index)

                    case CONSTANT_InvokeDynamic_ID =>
                        val CONSTANT_InvokeDynamic_info(bootstrapIndex, nameAndTypeIndex) = cpEntry
                        da.CONSTANT_InvokeDynamic_info(bootstrapIndex, nameAndTypeIndex)

                    case CONSTANT_Module_ID =>
                        val CONSTANT_Module_info(nameIndex) = cpEntry
                        da.CONSTANT_Module_info(nameIndex)

                    case CONSTANT_Package_ID =>
                        val CONSTANT_Package_info(nameIndex) = cpEntry
                        da.CONSTANT_Package_info(nameIndex)

                    case CONSTANT_Dynamic_ID =>
                        val CONSTANT_Dynamic_info(bootstrapIndex, nameAndTypeIndex) = cpEntry
                        da.CONSTANT_Dynamic_info(bootstrapIndex, nameAndTypeIndex)

                }
            }
        }
    }

    implicit class BRConstantsBuffer(val constantPool: Array[Constant_Pool_Entry]) extends AnyVal {
        def toDA: Array[da.Constant_Pool_Entry] = ba.toDA(constantPool)
    }

}
