/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische 
*    Universität Darmstadt nor the names of its contributors may be used to 
*    endorse or promote products derived from this software without specific 
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat
package canonical

import scala.reflect.ClassTag
import de.tud.cs.st.bat.reader.Constant_PoolReader
import de.tud.cs.st.bat.ConstantPoolValueAsString

/**
 * Representation of the constant pool as specified by the JVM Specification.
 * (This representation does not provide any abstraction.)
 *
 * @author Michael Eichberg
 */
trait ConstantPoolEntries {

    trait Constant_Pool_Entry {
        // TODO rename Constant_Type_Value => Constant_Kind
        def Constant_Type_Value: ConstantPoolTag

    }

    val Constant_Pool_EntryManifest: ClassTag[Constant_Pool_Entry] = implicitly

    case class CONSTANT_Class_info(val name_index: Int) extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_Class
    }

    case class CONSTANT_Double_info(val value: Double) extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_Double
    }

    case class CONSTANT_Float_info(val value: Float) extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_Float
    }

    case class CONSTANT_Integer_info(val value: Int) extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_Integer
    }

    case class CONSTANT_Long_info(val value: Long) extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_Long
    }

    case class CONSTANT_Utf8_info(val value: String) extends Constant_Pool_Entry with ConstantPoolValueAsString {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_Utf8
    }

    case class CONSTANT_String_info(val string_index: Int) extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_String
    }

    case class CONSTANT_Fieldref_info(val class_index: Int,
                                      val name_and_type_index: Int)
            extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_Fieldref
    }

    case class CONSTANT_Methodref_info(class_index: Int,
                                       name_and_type_index: Int)
            extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_Methodref
    }

    case class CONSTANT_InterfaceMethodref_info(class_index: Int,
                                                name_and_type_index: Int)
            extends Constant_Pool_Entry {

        def Constant_Type_Value = ConstantPoolTags.CONSTANT_InterfaceMethodref
    }

    case class CONSTANT_NameAndType_info(val name_index: Int,
                                         val descriptor_index: Int)
            extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_NameAndType
    }

    case class CONSTANT_MethodHandle_info(val reference_kind: ReferenceKind.Value,
                                          val reference_index: Int)
            extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_MethodHandle
    }

    case class CONSTANT_MethodType_info(val descriptor_index: Int)
            extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_MethodType
    }

    case class CONSTANT_InvokeDynamic_info(bootstrap_method_attr_index: Int,
                                           name_and_type_index: Int)
            extends Constant_Pool_Entry {
        def Constant_Type_Value = ConstantPoolTags.CONSTANT_InvokeDynamic
    }

}
object ConstantPoolEntries extends ConstantPoolEntries

