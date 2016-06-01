/* BSD 2-Clause License:
 * Copyright (c) 2016
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
package bc

import org.opalj.da._
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import org.opalj.bi.{ ConstantPoolTags ⇒ CPTags }
import org.opalj.da.ClassFileReader.LineNumberTable_attribute

/**
 * Assembles the specified class file(s).
 *
 * @author Michael Eichberg
 */
object Assembler {

    def as[T](x: AnyRef): T = x.asInstanceOf[T]

    implicit object RichCONSTANT_Class_info extends ClassFileElement[CONSTANT_Class_info] {
        def write(ci: CONSTANT_Class_info)(implicit out: DataOutputStream): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(name_index)
        }
    }

    implicit object RichCONSTANT_Ref extends ClassFileElement[CONSTANT_Ref] {
        def write(cr: CONSTANT_Ref)(implicit out: DataOutputStream): Unit = {
            import cr._
            import out._
            writeByte(tag)
            writeShort(class_index)
            writeShort(name_and_type_index)
        }
    }

    implicit object RichCONSTANT_String_info extends ClassFileElement[CONSTANT_String_info] {
        def write(ci: CONSTANT_String_info)(implicit out: DataOutputStream): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(string_index)
        }
    }

    implicit object RichCONSTANT_Integer_info extends ClassFileElement[CONSTANT_Integer_info] {
        def write(ci: CONSTANT_Integer_info)(implicit out: DataOutputStream): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeInt(value)
        }
    }

    implicit object RichCONSTANT_Float_info extends ClassFileElement[CONSTANT_Float_info] {
        def write(ci: CONSTANT_Float_info)(implicit out: DataOutputStream): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeFloat(value)
        }
    }

    implicit object RichCONSTANT_Long_info extends ClassFileElement[CONSTANT_Long_info] {
        def write(ci: CONSTANT_Long_info)(implicit out: DataOutputStream): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeLong(value)
        }
    }

    implicit object RichCONSTANT_Double_info extends ClassFileElement[CONSTANT_Double_info] {
        def write(ci: CONSTANT_Double_info)(implicit out: DataOutputStream): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeDouble(value)
        }
    }

    implicit object RichCONSTANT_NameAndType_info extends ClassFileElement[CONSTANT_NameAndType_info] {
        def write(ci: CONSTANT_NameAndType_info)(implicit out: DataOutputStream): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(name_index)
            writeShort(descriptor_index)

        }
    }

    implicit object RichCONSTANT_Utf8_info extends ClassFileElement[CONSTANT_Utf8_info] {
        def write(ci: CONSTANT_Utf8_info)(implicit out: DataOutputStream): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeUTF(value)
        }
    }

    implicit object RichCONSTANT_MethodHandle_info extends ClassFileElement[CONSTANT_MethodHandle_info] {
        def write(ci: CONSTANT_MethodHandle_info)(implicit out: DataOutputStream): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(reference_kind)
            writeShort(reference_index)
        }
    }

    implicit object RichCONSTANT_MethodType_info extends ClassFileElement[CONSTANT_MethodType_info] {
        def write(ci: CONSTANT_MethodType_info)(implicit out: DataOutputStream): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(descriptor_index)
        }
    }

    implicit object RichCONSTANT_InvokeDynamic_info extends ClassFileElement[CONSTANT_InvokeDynamic_info] {
        def write(ci: CONSTANT_InvokeDynamic_info)(implicit out: DataOutputStream): Unit = {
            import ci._
            import out._
            writeByte(tag)
            writeShort(bootstrap_method_attr_index)
            writeShort(name_and_type_index)
        }
    }

    implicit object RichConstant_Pool_Entry extends ClassFileElement[Constant_Pool_Entry] {
        def write(cpe: Constant_Pool_Entry)(implicit out: DataOutputStream): Unit = {
            cpe.Constant_Type_Value.id match {
                case CPTags.CONSTANT_Class_ID ⇒ serializeAs[CONSTANT_Class_info](cpe)
                case CPTags.CONSTANT_Fieldref_ID |
                    CPTags.CONSTANT_Methodref_ID |
                    CPTags.CONSTANT_InterfaceMethodref_ID ⇒ serializeAs[CONSTANT_Ref](cpe)
                case CPTags.CONSTANT_String_ID        ⇒ serializeAs[CONSTANT_String_info](cpe)
                case CPTags.CONSTANT_Integer_ID       ⇒ serializeAs[CONSTANT_Integer_info](cpe)
                case CPTags.CONSTANT_Float_ID         ⇒ serializeAs[CONSTANT_Float_info](cpe)
                case CPTags.CONSTANT_Long_ID          ⇒ serializeAs[CONSTANT_Long_info](cpe)
                case CPTags.CONSTANT_Double_ID        ⇒ serializeAs[CONSTANT_Double_info](cpe)
                case CPTags.CONSTANT_NameAndType_ID   ⇒ serializeAs[CONSTANT_NameAndType_info](cpe)
                case CPTags.CONSTANT_Utf8_ID          ⇒ serializeAs[CONSTANT_Utf8_info](cpe)
                case CPTags.CONSTANT_MethodHandle_ID  ⇒ serializeAs[CONSTANT_MethodHandle_info](cpe)
                case CPTags.CONSTANT_MethodType_ID    ⇒ serializeAs[CONSTANT_MethodType_info](cpe)
                case CPTags.CONSTANT_InvokeDynamic_ID ⇒ serializeAs[CONSTANT_InvokeDynamic_info](cpe)
            }
        }
    }

    implicit object RichAttribute extends ClassFileElement[Attribute] {
        def write(a: Attribute)(implicit out: DataOutputStream): Unit = {
            import a._
            import out._
            writeShort(attribute_name_index)
            writeInt(attribute_length)
            a match {
                case a: AnnotationDefault_attribute ⇒
                case a: Annotations_attribute       ⇒
                case a: BootstrapMethods_attribute  ⇒
                case a: Deprecated_attribute      ⇒
                case a: EnclosingMethod_attribute ⇒
                case a: LineNumberTable_attribute                      ⇒
                case a: LocalVariableTable_attribute                   ⇒
                case a: LocalVariableTypeTable_attribute               ⇒
                case a: MethodParameters_attribute                     ⇒
                case a: RuntimeVisibleAnnotations_attribute            ⇒
                case a: RuntimeInvisibleAnnotations_attribute          ⇒
                case a: RuntimeVisibleParameterAnnotations_attribute   ⇒
                case a: RuntimeInvisibleParameterAnnotations_attribute ⇒
                case a: RuntimeVisibleTypeAnnotations_attribute        ⇒
                case a: RuntimeInvisibleTypeAnnotations_attribute      ⇒
                case a: Signature_attribute                            ⇒
                case a: SourceDebugExtension_attribute                 ⇒
                case a: StackMapTable_attribute                        ⇒
                case a: Synthetic_attribute                            ⇒
                    
                case c: Code_attribute ⇒
                import c._
                writeShort(max_stack)
                writeShort(max_locals)
                val code_length = code.instructions.length
                writeInt(code_length)
                out.write(code.instructions, 0, code_length)
                writeShort(exceptionTable.length)
                exceptionTable.foreach { ex ⇒
                	import ex._
                	writeShort(start_pc)
                	writeShort(end_pc)
                	writeShort(handler_pc)
                	writeShort(catch_type)
                }
                writeShort(attributes.length)
                attributes.foreach { serialize(_) }
                                    
                case e: Exceptions_attribute ⇒
                import e._
                writeShort(exception_index_table.size)
                exception_index_table.foreach { writeShort(_) }
                
                case i: InnerClasses_attribute ⇒
                import i._
                writeShort(classes.size)
                classes.foreach { c ⇒
                	import c._
                	writeShort(inner_class_info_index)
                	writeShort(outer_class_info_index)
                	writeShort(inner_name_index)
                	writeShort(inner_class_access_flags)
                }
                
                case a: ConstantValue_attribute ⇒               writeShort(a.constantValue_index)

                case a: Unknown_attribute ⇒                    out.write(a.info, 0, a.info.length)

            }
        }
    }

    implicit object RichFieldInfo extends ClassFileElement[Field_Info] {
        def write(f: Field_Info)(implicit out: DataOutputStream): Unit = {
            import f._
            import out._
            writeShort(access_flags)
            writeShort(name_index)
            writeShort(descriptor_index)
            writeShort(attributes.size)
            attributes.foreach(serialize(_))
        }
    }

    implicit object RichMethodInfo extends ClassFileElement[Method_Info] {
        def write(m: Method_Info)(implicit out: DataOutputStream): Unit = {
            import m._
            import out._
            writeShort(access_flags)
            writeShort(name_index)
            writeShort(descriptor_index)
            writeShort(attributes.size)
            attributes.foreach(serialize(_))
        }
    }

    implicit object RichClassFile extends ClassFileElement[ClassFile] {

        def write(classFile: ClassFile)(implicit out: DataOutputStream): Unit = {
            import classFile._
            import out._
            writeInt(org.opalj.bi.ClassFileMagic)
            writeShort(minor_version)
            writeShort(major_version)
            writeShort(constant_pool.size + 1)
            constant_pool.tail.filter(_ ne null).foreach { serialize(_) }
            writeShort(access_flags)
            writeShort(this_class)
            writeShort(super_class)
            writeShort(interfaces.size)
            interfaces.foreach { writeShort(_) }
            writeShort(fields.size)
            fields.foreach { serialize(_) }
            writeShort(methods.size)
            methods.foreach { serialize(_) }
            writeShort(attributes.size)
            attributes.foreach { serialize(_) }
        }
    }

    /**
     * `serializeAs` enables you to specify the object type of the given parameter `t` and
     * that type will be used to pick up the implicit class file element value.
     */
    def serializeAs[T](t: AnyRef)(implicit out: DataOutputStream, cfe: ClassFileElement[T]): Unit = {
        cfe.write(as[T](t))
    }

    /**
     * You should use serialize if the concrete/required type of the given parameter is available/can
     * be automatically inferred by the Scala compiler.
     */
    def serialize[T: ClassFileElement](t: T)(implicit out: DataOutputStream): Unit = {
        implicitly[ClassFileElement[T]].write(t)
    }

    def apply(classFile: ClassFile): Array[Byte] = {
        val data = new ByteArrayOutputStream(classFile.size)
        implicit val out = new DataOutputStream(data)
        serialize(classFile)
        out.flush()
        data.toByteArray()
    }

}

trait ClassFileElement[T] {

    def write(t: T)(implicit out: DataOutputStream): Unit

}

