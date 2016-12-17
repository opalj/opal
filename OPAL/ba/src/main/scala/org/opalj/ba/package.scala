/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package org.opalj

import org.opalj.log.GlobalLogContext
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

import org.opalj.br.Attribute
import org.opalj.br.ClassFile
import org.opalj.br.Code
import org.opalj.br.ExceptionHandler
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodHandle
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.SourceFile
import org.opalj.br.cp._ // we need ALL of them...
import org.opalj.br.instructions._ // we need NEARY ALL of them...

/**
 * Implementation of an EDSL for creating Java bytecode. The EDSL is designed to facilitate
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
package object ba {

    {
        // Log the information whether a production build or a development build is used.
        implicit val logContext = GlobalLogContext
        import OPALLogger.info
        try {
            scala.Predef.assert(false)
            info("OPAL", "Bytecode Assembler - Production Build")
        } catch {
            case ae: AssertionError ⇒
                info("OPAL", "Bytecode Assembler - Development Build (Assertions are enabled)")
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

    // *********************************************************************************************
    //
    //          F U N C T I O N A L I T Y   T O  C R E A T E    B R C L A S S F I L E S
    //
    // *********************************************************************************************

    /**
     * Converts a [[org.opalj.br.ClassFile]] to a [[org.opalj.da.ClassFile]] and all its attributes
     * to the attributes in [[org.opalj.da]].
     */
    implicit class ClassConvert(classFile: ClassFile) {
        def assembleToDA: da.ClassFile = {
            val constantPoolBuffer = new ConstantPoolBuffer()
            val thisNamePos = constantPoolBuffer.CPEClass(classFile.thisType)
            val superClassPos = constantPoolBuffer.CPEClass(
                classFile.superclassType.getOrElse(ObjectType("java/lang/Object"))
            )

            val interfaces = classFile.interfaceTypes.map(constantPoolBuffer.CPEClass(_))
            val fields = classFile.fields.map(_.assembleToDA(constantPoolBuffer))
            val methods = classFile.methods.map(_.assembleToDA(constantPoolBuffer))
            val attributes = classFile.attributes.map(_.assembleToDA(constantPoolBuffer))

            da.ClassFile(
                constant_pool = constantPoolBuffer.assembleToDA,
                minor_version = classFile.version.minor,
                major_version = classFile.version.major,
                access_flags = classFile.accessFlags,
                this_class = thisNamePos,
                super_class = superClassPos,
                interfaces = interfaces.toIndexedSeq,
                fields = fields,
                methods = methods,
                attributes = attributes
            )
        }
    }

    private implicit class FieldConvert(field: Field) {
        def assembleToDA(constantPoolBuffer: ConstantPoolBuffer): da.Field_Info = {
            da.Field_Info(
                access_flags = field.accessFlags,
                name_index = constantPoolBuffer.CPEUtf8(field.name),
                descriptor_index = constantPoolBuffer.CPEUtf8(field.fieldType.toJVMTypeName),
                attributes = field.attributes.map(_.assembleToDA(constantPoolBuffer))
            )
        }
    }

    private implicit class MethodConvert(method: Method) {
        def assembleToDA(constantPoolBuffer: ConstantPoolBuffer): da.Method_Info = {
            var attributes = method.attributes.map(_.assembleToDA(constantPoolBuffer))
            if (method.body.isDefined) {
                attributes = attributes :+ method.body.get.assembleToDA(constantPoolBuffer)
            }
            da.Method_Info(
                access_flags = method.accessFlags,
                name_index = constantPoolBuffer.CPEUtf8(method.name),
                descriptor_index = constantPoolBuffer.CPEUtf8(method.descriptor.toJVMDescriptor),
                attributes = attributes
            )
        }
    }

    private implicit class CodeConvert(code: Code) {
        def assembleToDA(constantPoolBuffer: ConstantPoolBuffer): da.Code_attribute = {
            val instructions = scala.collection.mutable.ListBuffer.empty[Byte]

            def addShortArg(value: Int) = {
                instructions.append((value >>> 8).toByte)
                instructions.append((value & 0xFF).toByte)
            }

            def addIntArg(value: Int) = {
                instructions.append((value >>> 24).toByte)
                instructions.append((value >>> 16).toByte)
                instructions.append((value >>> 8).toByte)
                instructions.append((value & 0xFF).toByte)
            }

            var nextPC = 0
            var modifiedByWide = false
            code.foreach(e ⇒ {
                val (_, i) = e
                instructions.append(i.opcode.toByte)
                i match {
                    case BIPUSH(value) ⇒ addShortArg(value)
                    case CHECKCAST(referenceType) ⇒ {
                        val argIndex = constantPoolBuffer.CPEClass(referenceType)
                        addShortArg(argIndex)
                    }
                    case IINC(lvIndex, constValue) ⇒ {
                        if (modifiedByWide) {
                            addShortArg(lvIndex)
                            addShortArg(constValue)
                        } else {
                            instructions.append(lvIndex.toByte)
                            instructions.append(constValue.toByte)
                        }
                    }
                    case INSTANCEOF(referenceType) ⇒ {
                        val argIndex = constantPoolBuffer.CPEClass(referenceType)
                        addShortArg(argIndex)
                    }
                    case JSR_W(branchoffset) ⇒ addIntArg(branchoffset)
                    case _: LDC[_] | _: LDC_W[_] ⇒ {
                        val value = i.asInstanceOf[LoadConstantInstruction[_]].value
                        val argIndex = value match {
                            case int: Int ⇒ {
                                constantPoolBuffer.CPEInteger(int)
                            }
                            case float: Float ⇒ {
                                constantPoolBuffer.CPEFloat(float)
                            }
                            case string: String ⇒ {
                                constantPoolBuffer.CPEString(string)
                            }
                            case mh: MethodHandle ⇒ {
                                constantPoolBuffer.CPEMethodHandle(mh)
                            }
                            case md: MethodDescriptor ⇒ {
                                constantPoolBuffer.CPEMethodType(md.toJVMDescriptor)
                            }
                            case reference: ReferenceType ⇒ {
                                constantPoolBuffer.CPEClass(reference)
                            }
                        }

                        if (i.isInstanceOf[LDC_W[_]]) {
                            addIntArg(argIndex)
                        } else {
                            addShortArg(argIndex)
                        }
                    }
                    case l: LDC2_W[_] ⇒ {
                        l.value match {
                            case l: Long ⇒ {
                                addShortArg(constantPoolBuffer.CPELong(l))
                            }
                            case d: Double ⇒ {
                                addShortArg(constantPoolBuffer.CPEDouble(d))
                            }
                        }
                    }
                    //TODO: LOOKUPSWITCH, MULTIANEWARRAY
                    case NEW(objectType) ⇒ {
                        val argIndex = constantPoolBuffer.CPEClass(objectType)
                        addShortArg(argIndex)
                    }
                    //TODO: NEWARRAY
                    case RET(lvIndex)  ⇒ instructions.append(lvIndex.toByte)
                    case SIPUSH(value) ⇒ addShortArg(value)
                    //TODO: TABLESWITCH

                    //TODO: INVOKEDYNAMIC
                    case methodInvocation: MethodInvocationInstruction ⇒
                        val argIndex = constantPoolBuffer.CPEMethodRef(
                            methodInvocation.declaringClass,
                            methodInvocation.name,
                            methodInvocation.methodDescriptor.toJVMDescriptor
                        )
                        addShortArg(argIndex)
                    case s: SimpleConditionalBranchInstruction ⇒ addShortArg(s.branchoffset)
                    case u: UnconditionalBranchInstruction     ⇒ addShortArg(u.branchoffset)
                    case f: FieldAccess ⇒ {
                        val argIndex = constantPoolBuffer.CPEFieldRef(
                            f.declaringClass,
                            f.name,
                            f.fieldType.toJVMTypeName
                        )
                        addShortArg(argIndex)
                    }
                    case e: ExplicitLocalVariableIndex ⇒ {
                        if (modifiedByWide) {
                            addShortArg(e.lvIndex)
                        } else {
                            instructions.append(e.lvIndex.toByte)
                        }
                    }
                    case _ ⇒
                }

                modifiedByWide = false
                if (i.isInstanceOf[WIDE.type]) {
                    modifiedByWide = true
                }

                nextPC = i.indexOfNextInstruction(nextPC)(code)
            })

            da.Code_attribute(
                attribute_name_index = constantPoolBuffer.CPEUtf8("Code"),
                max_stack = code.maxStack,
                max_locals = code.maxLocals,
                code = da.Code(instructions.toArray),
                exceptionTable = code.exceptionHandlers.map(e ⇒ e.assembleToDA(constantPoolBuffer)),
                attributes = IndexedSeq.empty
            )
        }
    }

    private implicit class ExceptionHandlersConvert(exceptionHandler: ExceptionHandler) {
        def assembleToDA(constantPoolBuffer: ConstantPoolBuffer): da.ExceptionTableEntry = {
            val index = if (exceptionHandler.catchType.isDefined) {
                constantPoolBuffer.CPEClass(exceptionHandler.catchType.get)
            } else 0
            da.ExceptionTableEntry(
                exceptionHandler.startPC,
                exceptionHandler.endPC,
                exceptionHandler.handlerPC,
                index
            )
        }
    }

    private implicit class AttributeConvert(attribute: Attribute) {
        def assembleToDA(constantPoolBuffer: ConstantPoolBuffer): da.Attribute = {
            attribute match {
                case c: Code ⇒ c.assembleToDA(constantPoolBuffer)
                case SourceFile(s) ⇒ da.SourceFile_attribute(
                    constantPoolBuffer.CPEUtf8("SourceFile"),
                    constantPoolBuffer.CPEUtf8(s)
                )
            }
        }
    }

    private implicit class ConstantPoolConvert(constantPoolBuffer: ConstantPoolBuffer) {
        def assembleToDA: Array[da.Constant_Pool_Entry] = {
            constantPoolBuffer.toArray.map {
                case CONSTANT_NameAndType_info(name_index, descriptor_index) ⇒ {
                    da.CONSTANT_NameAndType_info(
                        name_index,
                        descriptor_index
                    )
                }
                case CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index) ⇒ {
                    da.CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index)
                }
                case CONSTANT_Fieldref_info(class_index, name_and_type_index) ⇒ {
                    da.CONSTANT_Fieldref_info(
                        class_index,
                        name_and_type_index
                    )
                }
                case CONSTANT_MethodHandle_info(referenceKind, referenceIndex) ⇒ {
                    da.CONSTANT_MethodHandle_info(
                        referenceKind,
                        referenceIndex
                    )
                }
                case CONSTANT_Methodref_info(class_index, name_and_type_index) ⇒ {
                    da.CONSTANT_Methodref_info(
                        class_index,
                        name_and_type_index
                    )
                }
                case CONSTANT_InvokeDynamic_info(bootstrapMethodIndex, nameAndTypeIndex) ⇒ {
                    da.CONSTANT_InvokeDynamic_info(
                        bootstrapMethodIndex,
                        nameAndTypeIndex
                    )
                }

                case CONSTANT_Class_info(name_index) ⇒ da.CONSTANT_Class_info(name_index)
                case CONSTANT_MethodType_info(index) ⇒ da.CONSTANT_MethodType_info(index)

                case CONSTANT_Double_info(d)         ⇒ da.CONSTANT_Double_info(d.value)
                case CONSTANT_Float_info(f)          ⇒ da.CONSTANT_Float_info(f.value)
                case CONSTANT_Long_info(l)           ⇒ da.CONSTANT_Long_info(l.value)
                case CONSTANT_Integer_info(i)        ⇒ da.CONSTANT_Integer_info(i.value)
                case CONSTANT_String_info(s)         ⇒ da.CONSTANT_String_info(s)
                case CONSTANT_Utf8_info(u)           ⇒ da.CONSTANT_Utf8(u)
                case null                            ⇒ null
                case _                               ⇒ throw new IllegalArgumentException
            }
        }
    }

}
