/* License (BSD Style License):
 * Copyright (c) 2009, 2011
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Software Technology Group or Technische
 *   Universität Darmstadt nor the names of its contributors may be used to
 *   endorse or promote products derived from this software without specific
 *   prior written permission.
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
package de.tud.cs.st.bat.resolved
package reader

import de.tud.cs.st.bat.canonical.reader.Constant_PoolBinding

import de.tud.cs.st.bat.resolved.FieldDescriptor
import de.tud.cs.st.bat.resolved.MethodDescriptor

/**
 * Helper methods to replace int-based references  into the constant pool
 * (i.e., Constant_Pool_Indexes) with
 * direct reference to the corresponding, decoded objects.
 *
 * @author Michael Eichberg
 */
trait Constant_PoolResolver extends Constant_PoolBinding {

    // ______________________________________________________________________________________________
    //
    // VARIOUS HELPER METHODS THAT ARE - IN GENERAL - IMPLICITLY USED TO RESOLVE THE CONSTANT POOL.
    //
    // The following implicit method definitions use the target type (e.g. ObjectType,
    // FieldDescriptor,...) to determine the type of the constant pool's entry with the specified
    // index.
    // All references to the constant pool will be resolved while parsing the class file. After
    // the class file is read the constant pool can be / is deleted.
    // ______________________________________________________________________________________________
    //

    implicit def CONSTANT_NameAndType_info_IndexToNameAndMethodDescriptor(cntii: Constant_Pool_Index)(implicit cp: Constant_Pool): (String, MethodDescriptor) =
        cp(cntii) match {
            case cnti: CONSTANT_NameAndType_info ⇒ (
                cnti.name_index, // => implicitly converted
                cnti.descriptor_index // => implicitly converted
            )
        }

    implicit def CONSTANT_NameAndType_info_IndexToNameAndFieldType(cntii: Constant_Pool_Index)(implicit cp: Constant_Pool): (String, FieldType) =
        cp(cntii) match {
            case cnti: CONSTANT_NameAndType_info ⇒ (
                cnti.name_index, // => implicitly converted
                {
                    CONSTANT_Utf8_info_IndexToFieldDescriptor(cnti.descriptor_index).fieldType
                }
            )
        }

    implicit def CONSTANT_Fieldref_info_IndexToFieldref(cfrii: Constant_Pool_Index)(implicit cp: Constant_Pool): (ObjectType, String, FieldType) =
        cp(cfrii) match {
            case cfri: CONSTANT_Fieldref_info ⇒
                val declaringClass: ObjectType = cfri.class_index // => implicitly converted
                val (name, fieldType) = CONSTANT_NameAndType_info_IndexToNameAndFieldType(cfri.name_and_type_index)
                (declaringClass, name, fieldType)
        }

    implicit def CONSTANT_MethodRef_info_IndexToMethodRef(cmrii: Constant_Pool_Index)(implicit cp: Constant_Pool): (ObjectType, String, MethodDescriptor) =
        cp(cmrii) match {
            case cmri: CONSTANT_Methodref_info ⇒
                val declaringClass: ObjectType = cmri.class_index
                val (name, methodDescriptor) = CONSTANT_NameAndType_info_IndexToNameAndMethodDescriptor(cmri.name_and_type_index)
                (declaringClass, name, methodDescriptor)
            case cimri: CONSTANT_InterfaceMethodref_info ⇒
                val declaringClass: ObjectType = cimri.class_index
                val (name, methodDescriptor) = CONSTANT_NameAndType_info_IndexToNameAndMethodDescriptor(cimri.name_and_type_index)
                (declaringClass, name, methodDescriptor)
        }

    implicit def CONSTANT_Class_info_IndexToObjectType(ccii: Constant_Pool_Index)(implicit cp: Constant_Pool): ObjectType =
        cp(ccii) match {
            case cci: CONSTANT_Class_info ⇒
                cp(cci.name_index) match { case cui: CONSTANT_Utf8_info ⇒ ObjectType(cui.value) }
        }

    implicit def CONSTANT_Utf8_info_IndexToString(cuii: Constant_Pool_Index)(implicit cp: Constant_Pool): String =
        cp(cuii) match { case cui: CONSTANT_Utf8_info ⇒ cui.value }

    implicit def CONSTANT_Utf8_info_IndexToFieldDescriptor(cuii: Constant_Pool_Index)(implicit cp: Constant_Pool): FieldDescriptor =
        cp(cuii) match { case cui: CONSTANT_Utf8_info ⇒ FieldDescriptor(cui.value) }

    implicit def CONSTANT_Utf8_info_IndexToMethodDescriptor(cuii: Constant_Pool_Index)(implicit cp: Constant_Pool): MethodDescriptor =
        cp(cuii) match { case cui: CONSTANT_Utf8_info ⇒ MethodDescriptor(cui.value) }

    implicit def CONSTANT_Value_IndexToConstantValue(cvi: Constant_Pool_Index)(implicit cp: Constant_Pool): ConstantValue[_] =
        cp(cvi) match {
            case csi: CONSTANT_String_info  ⇒ ConstantString(csi.string_index /* implicit conversion */ )
            case cli: CONSTANT_Long_info    ⇒ ConstantLong(cli.value)
            case cii: CONSTANT_Integer_info ⇒ ConstantInteger(cii.value)
            case cdi: CONSTANT_Double_info  ⇒ ConstantDouble(cdi.value)
            case cfi: CONSTANT_Float_info   ⇒ ConstantFloat(cfi.value)
            case cu8i: CONSTANT_Utf8_info   ⇒ ConstantString(cu8i.value) // added to support annotations
            case cci: CONSTANT_Class_info ⇒
                cp(cci.name_index) match {
                    case cui: CONSTANT_Utf8_info ⇒ ConstantClass(
                        {
                            val s = cui.value
                            if (s.charAt(0) == '[')
                                FieldType(s).asInstanceOf[ReferenceType]
                            else
                                ObjectType(s)
                        }
                    )
                }
        }

    implicit def SeqOfCONSTANT_Class_info_IndexToSeqOfObjectType(cciis: Seq[Constant_Pool_Index])(implicit cp: Constant_Pool): Seq[ObjectType] =
        cciis map (
            cp(_) match {
                case cci: CONSTANT_Class_info ⇒
                    cp(cci.name_index) match { case cui: CONSTANT_Utf8_info ⇒ ObjectType(cui.value) }
            }
        )

    implicit def FieldDescriptorToFieldType(fd: FieldDescriptor): FieldType = fd.fieldType

    implicit def CONSTANT_Utf8_info_IndexToSignature(cuii: Constant_Pool_Index)(implicit cp: Constant_Pool, ap: de.tud.cs.st.bat.reader.AttributesParent.Value): Signature = {
        import de.tud.cs.st.bat.reader.AttributesParent
        ap match {
            case AttributesParent.Field_info     ⇒ SignatureParser.parseFieldTypeSignature(cuii)
            case AttributesParent.ClassFile      ⇒ SignatureParser.parseClassSignature(cuii)
            case AttributesParent.Method_info    ⇒ SignatureParser.parseMethodTypeSignature(cuii)
            case AttributesParent.Code_attribute ⇒ sys.error("signature attributes stored in a code_attribute's attributes table are non-standard")
        }
    }

    def cpidxToFieldTypeSignature(signature_index: Constant_Pool_Index)(implicit cp: Constant_Pool): FieldTypeSignature = {
        SignatureParser.parseFieldTypeSignature(signature_index)
    }
}


