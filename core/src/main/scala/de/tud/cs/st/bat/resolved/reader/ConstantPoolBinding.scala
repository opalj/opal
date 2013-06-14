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
package de.tud.cs.st.bat
package resolved
package reader

import scala.language.implicitConversions
import scala.reflect.ClassTag
import de.tud.cs.st.bat.reader.Constant_PoolReader

/**
  * A representation of the constant pool.
  *
  * @note The constant pool is considered to be static; i.e., references between
  * constant pool entries are always resolved at most once and the results are cached.
  * Hence, after reading the constant pool the constant pool is treated as
  * immutable; the referenced constant pool entry must not change.
  * @author Michael Eichberg
  */
trait ConstantPoolBinding extends Constant_PoolReader {

    implicit def ConstantPoolIndexToConstantPoolEntry(index: Constant_Pool_Index)(implicit cp: Constant_Pool): Constant_Pool_Entry =
        cp(index)

    trait Constant_Pool_Entry {
        def asString: String = sys.error("conversion to string is not supported")
        def asFieldType: FieldType = sys.error("conversion to field type is not supported")
        def asMethodDescriptor: MethodDescriptor = sys.error("conversion to method descriptor is not supported")
        def asFieldTypeSignature: FieldTypeSignature = sys.error("conversion to field type signature is not supported")

        def asSignature(implicit ap: AttributeParent): Signature = sys.error("conversion to signature attribute is not supported")

        def asNameAndMethodDescriptor(implicit cp: Constant_Pool): (String, MethodDescriptor) = sys.error("conversion to name and method descriptor is not supported")
        def asConstantValue(implicit cp: Constant_Pool): ConstantValue[_] = sys.error("conversion to constant value is not supported")
        def asFieldref(implicit cp: Constant_Pool): (ObjectType, String, FieldType) = sys.error("conversion to field ref is not supported")
        def asMethodref(implicit cp: Constant_Pool): (ReferenceType, String, MethodDescriptor) = sys.error("conversion to method ref is not supported")
        def asInvoke[T <: Instruction](create: (ReferenceType, String, MethodDescriptor) ⇒ T)(implicit cp: Constant_Pool): T = sys.error("conversion to method ref is not supported")

        def asObjectType(implicit cp: Constant_Pool): ObjectType = sys.error("conversion to object type is not supported")
        def asReferenceType(implicit cp: Constant_Pool): ReferenceType = sys.error("conversion to object type is not supported")

        def asNameAndType: CONSTANT_NameAndType_info = sys.error("conversion to name and type info is not supported")
    }

    val Constant_Pool_EntryManifest: ClassTag[Constant_Pool_Entry] = implicitly

    case class CONSTANT_Class_info(val name_index: Constant_Pool_Index) extends Constant_Pool_Entry {
        override def asConstantValue(implicit cp: Constant_Pool) = ConstantClass(asReferenceType)
        override def asObjectType(implicit cp: Constant_Pool) = ObjectType(name_index.asString)
        override def asReferenceType(implicit cp: Constant_Pool) = ReferenceType(name_index.asString)
    }

    case class CONSTANT_Double_info(value: ConstantDouble) extends Constant_Pool_Entry {
        def this(value: Double) { this(ConstantDouble(value)) }
        override def asConstantValue(implicit cp: Constant_Pool) = value
    }

    case class CONSTANT_Float_info(value: ConstantFloat) extends Constant_Pool_Entry {
        def this(value: Float) { this(ConstantFloat(value)) }
        override def asConstantValue(implicit cp: Constant_Pool) = value
    }

    case class CONSTANT_Integer_info(value: ConstantInteger) extends Constant_Pool_Entry {
        def this(value: Int) { this(ConstantInteger(value)) }
        override def asConstantValue(implicit cp: Constant_Pool) = value
    }

    case class CONSTANT_Long_info(value: ConstantLong) extends Constant_Pool_Entry {
        def this(value: Long) { this(ConstantLong(value)) }
        override def asConstantValue(implicit cp: Constant_Pool) = value
    }

    case class CONSTANT_Utf8_info(val value: String) extends Constant_Pool_Entry with ConstantPoolValueAsString {

        override def asString = value

        private[this] var methodDescriptor: MethodDescriptor = null // to cache the result
        override def asMethodDescriptor = {
            if (methodDescriptor eq null) { methodDescriptor = MethodDescriptor(value) };
            methodDescriptor
        }

        private[this] var fieldType: FieldType = null // to cache the result
        override def asFieldType = {
            if (fieldType eq null) { fieldType = FieldType(value) };
            fieldType
        }

        override def asFieldTypeSignature = SignatureParser.parseFieldTypeSignature(value) // should be called at most once => caching doesn't make sense

        override def asSignature(implicit ap: AttributeParent) = { // should be called at most once => caching doesn't make sense
            ap match {
                case AttributesParent.Field     ⇒ SignatureParser.parseFieldTypeSignature(value)
                case AttributesParent.ClassFile ⇒ SignatureParser.parseClassSignature(value)
                case AttributesParent.Method    ⇒ SignatureParser.parseMethodTypeSignature(value)
                case AttributesParent.Code      ⇒ sys.error("signature attributes stored in a code_attribute's attributes table are non-standard")
            }
        }
        override def asConstantValue(implicit cp: Constant_Pool) = ConstantString(value) // required to support annotations; should be called at most once => caching doesn't make sense
    }

    case class CONSTANT_String_info(val string_index: Constant_Pool_Index) extends Constant_Pool_Entry {
        override def asConstantValue(implicit cp: Constant_Pool) = ConstantString(string_index.asString)
    }

    case class CONSTANT_Fieldref_info(val class_index: Constant_Pool_Index,
                                      val name_and_type_index: Constant_Pool_Index)
            extends Constant_Pool_Entry {

        private[this] var fieldref: (ObjectType, String, FieldType) = null // to cache the result
        override def asFieldref(implicit cp: Constant_Pool): (ObjectType, String, FieldType) = {
            if (fieldref eq null) {
                val nameAndType = name_and_type_index.asNameAndType
                fieldref = (class_index.asObjectType,
                    nameAndType.name,
                    nameAndType.fieldType
                )
            }
            fieldref
        }
    }

    private[ConstantPoolBinding] trait AsMethodref extends Constant_Pool_Entry {

        def class_index: Constant_Pool_Index
        def name_and_type_index: Constant_Pool_Index

        private[this] var methodref: (ReferenceType, String, MethodDescriptor) = null // to cache the result
        override def asMethodref(implicit cp: Constant_Pool): (ReferenceType, String, MethodDescriptor) = {
            if (methodref eq null) {
                val nameAndType = name_and_type_index.asNameAndType
                methodref = (class_index.asReferenceType,
                    nameAndType.name,
                    nameAndType.methodDescriptor
                )
            }
            methodref
        }
        override def asInvoke[T <: Instruction](create: (ReferenceType, String, MethodDescriptor) ⇒ T)(implicit cp: Constant_Pool): T = {
            val nameAndType = name_and_type_index.asNameAndType
            create(class_index.asReferenceType, nameAndType.name, nameAndType.methodDescriptor)
        }
    }

    case class CONSTANT_Methodref_info(class_index: Constant_Pool_Index,
                                       name_and_type_index: Constant_Pool_Index) extends AsMethodref

    case class CONSTANT_InterfaceMethodref_info(class_index: Constant_Pool_Index,
                                                name_and_type_index: Constant_Pool_Index) extends AsMethodref

    case class CONSTANT_NameAndType_info(name_index: Constant_Pool_Index,
                                         descriptor_index: Constant_Pool_Index)
            extends Constant_Pool_Entry {

        override def asNameAndType: CONSTANT_NameAndType_info = this

        def name(implicit cp: Constant_Pool): String = name_index.asString // this operation is very cheap and hence, it doesn't make sense to cache the result
        def fieldType(implicit cp: Constant_Pool): FieldType = descriptor_index.asFieldType
        def methodDescriptor(implicit cp: Constant_Pool): MethodDescriptor = descriptor_index.asMethodDescriptor
    }

    case class CONSTANT_MethodHandle_info(val reference_kind: Int,
                                          val reference_index: Int)
            extends Constant_Pool_Entry {

        // TODO [Java 7] Support CONSTANT_MethodHandle_info
    }

    case class CONSTANT_MethodType_info(val descriptor_index: Constant_Pool_Index)
            extends Constant_Pool_Entry {

        // TODO [Java 7] Support CONSTANT_MethodType_info
    }

    case class CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index: Constant_Pool_Index,
                                           name_and_type_index: Constant_Pool_Index)
            extends Constant_Pool_Entry {
        // TODO [Java 7] Support CONSTANT_InvokeDynamic
    }

    //
    // IMPLEMENTATION OF THE CONSTANT POOL READER'S FACTORY METHODS
    //

    def CONSTANT_Class_info(i: Int): CONSTANT_Class_info = new CONSTANT_Class_info(i)

    def CONSTANT_Double_info(d: Double): CONSTANT_Double_info = new CONSTANT_Double_info(d)

    def CONSTANT_Float_info(f: Float): CONSTANT_Float_info = new CONSTANT_Float_info(f)

    def CONSTANT_Integer_info(i: Int): CONSTANT_Integer_info = new CONSTANT_Integer_info(i)

    def CONSTANT_Long_info(l: Long): CONSTANT_Long_info = new CONSTANT_Long_info(l)

    def CONSTANT_Utf8_info(s: String): CONSTANT_Utf8_info = new CONSTANT_Utf8_info(s)

    def CONSTANT_String_info(i: Int): CONSTANT_String_info = new CONSTANT_String_info(i)

    def CONSTANT_Fieldref_info(class_index: Constant_Pool_Index,
                               name_and_type_index: Constant_Pool_Index): CONSTANT_Fieldref_info =
        new CONSTANT_Fieldref_info(class_index, name_and_type_index)

    def CONSTANT_Methodref_info(class_index: Constant_Pool_Index,
                                name_and_type_index: Constant_Pool_Index): CONSTANT_Methodref_info =
        new CONSTANT_Methodref_info(class_index, name_and_type_index)

    def CONSTANT_InterfaceMethodref_info(class_index: Constant_Pool_Index,
                                         name_and_type_index: Constant_Pool_Index): CONSTANT_InterfaceMethodref_info =
        new CONSTANT_InterfaceMethodref_info(class_index, name_and_type_index)

    def CONSTANT_NameAndType_info(name_index: Constant_Pool_Index,
                                  descriptor_index: Constant_Pool_Index): CONSTANT_NameAndType_info =
        new CONSTANT_NameAndType_info(name_index, descriptor_index)

    def CONSTANT_MethodHandle_info(reference_kind: Int,
                                   reference_index: Int): CONSTANT_MethodHandle_info =
        new CONSTANT_MethodHandle_info(reference_kind, reference_index)

    def CONSTANT_MethodType_info(descriptor_index: Constant_Pool_Index): CONSTANT_MethodType_info =
        new CONSTANT_MethodType_info(descriptor_index)

    def CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index: Constant_Pool_Index,
                                    name_and_type_index: Constant_Pool_Index): CONSTANT_InvokeDynamic_info =
        new CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index, name_and_type_index)

}


