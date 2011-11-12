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
package de.tud.cs.st.bat.canonical.reader

import de.tud.cs.st.bat.reader.Unknown_attributeReader


/**
 * Creates a one to one representation of a Java class files. I.e., the 
 * constant pool is represented as is and all references into the constant pool
 * are left as is.
 *
 * @author Michael Eichberg
 */
object BasicJava6Framework 
	extends de.tud.cs.st.bat.reader.Java6Reader 
		with Constant_PoolBinding
		with Unknown_attributeReader {


	// 
	// IMPLEMENTATION OF THE CLASSES REPRESENTING A CLASS FILE
	//


	type Attribute = de.tud.cs.st.bat.canonical.Attribute


	type ElementValue = de.tud.cs.st.bat.canonical.ElementValue

	val VerificationTypeInfoManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.VerificationTypeInfo] = implicitly
	val StackMapFrameManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.StackMapFrame]= implicitly
	val ElementValuePairManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.ElementValuePair]= implicitly
	val ElementValueManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.ElementValue]= implicitly
	val AnnotationManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.Annotation]= implicitly
	val ExceptionTableEntryManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.ExceptionTableEntry]= implicitly
	val LocalVariableTypeTableEntryManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.LocalVariableTypeTableEntry]= implicitly
	val LocalVariableTableEntryManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.LocalVariableTableEntry]= implicitly
	val LineNumberTableEntryManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.LineNumberTableEntry]= implicitly
	val InnerClassesEntryManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.InnerClassesEntry]= implicitly
	val Exceptions_attributeManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.Exceptions_attribute]= implicitly
	val AttributeManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.Attribute]= implicitly
	val InterfaceManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.Interface] = implicitly
	val Method_InfoManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.Method_Info]= implicitly
	val Field_InfoManifest : ClassManifest[de.tud.cs.st.bat.canonical.reader.BasicJava6Framework.Field_Info]= implicitly


	case class ClassFile (
	  	val constant_pool : Constant_Pool,
		val minor_version : Int,
	  	val major_version : Int,
	  	val access_flags : Int,
	  	val this_class : Int,
	  	val super_class : Int,
	  	val interfaces: Interfaces, // TODO DELETE ME IndexedSeq[Constant_Pool_Index],//Seq[Constant_Pool_Index], // references (int values) to constant pool entries
	  	val fields : Fields,
	  	val methods : Methods,
	  	val attributes : Attributes
	) extends de.tud.cs.st.bat.canonical.ClassFile{

		type Constant_Pool_Entry = BasicJava6Framework.Constant_Pool_Entry
		type Fields = BasicJava6Framework.Fields
		type Methods = BasicJava6Framework.Methods
		type Attributes = BasicJava6Framework.Attributes
		type Interfaces = BasicJava6Framework.Interfaces
	}	

	def ClassFile(
		minor_version : Int, major_version : Int, 
		access_flags : Int, this_class : Int, super_class : Int,
		interfaces : Interfaces, fields : Fields, methods : Methods, attributes : Attributes
	) (implicit cp : Constant_Pool) : ClassFile = 
		new ClassFile(
			cp, minor_version, major_version, access_flags,
			this_class, super_class, interfaces , fields, methods, attributes
		) 


	type Interface = Constant_Pool_Index
	def Interface(interface_index : Constant_Pool_Index ) ( implicit constant_pool : Constant_Pool) : Interface =  interface_index
	
	
	case class Field_Info (
		val access_flags : Int,
	  	val name_index : Int,
	  	val descriptor_index : Int,
	  	val attributes : Attributes
	) extends de.tud.cs.st.bat.canonical.Field_Info {

		type Attributes = BasicJava6Framework.Attributes
	}	

	def Field_Info(
		access_flags : Int, name_index : Int, descriptor_index : Int, attributes : Attributes
	)( implicit constant_pool : Constant_Pool) : Field_Info =
		new Field_Info(
			access_flags, name_index, descriptor_index, attributes 
		) 


	case class Method_Info (
		val accessFlags : Int, 
		val name_index : Int,
		val descriptor_index : Int,
		val attributes : Attributes
	) extends de.tud.cs.st.bat.canonical.Method_Info {

		type Attributes = BasicJava6Framework.Attributes
	}

	def Method_Info(
		accessFlags: Int, name_index : Int, descriptor_index : Int,	attributes : Attributes
	)( implicit constant_pool : Constant_Pool) : Method_Info = 
		new Method_Info(
			accessFlags, name_index, descriptor_index, attributes
		)


	case class Unknown_attribute(
		val attribute_name_index : Int,
		val info : Array[Byte]
	) extends de.tud.cs.st.bat.canonical.Unknown_attribute 

	def Unknown_attribute(
		attribute_name_index : Int,
		info : Array[Byte]
	)( implicit constant_pool : Constant_Pool) : Unknown_attribute = {
		new Unknown_attribute(attribute_name_index /* , attribute_name*/, info	) 
	}


	case class SourceFile_attribute(
		val attribute_name_index : Int,
		val sourceFile_index : Int
	) extends de.tud.cs.st.bat.canonical.SourceFile_attribute 

	def SourceFile_attribute(
		attribute_name_index : Int, sourceFile_index : Int
	)( implicit constant_pool : Constant_Pool) : SourceFile_attribute = new SourceFile_attribute(attribute_name_index , sourceFile_index)


	case class InnerClasses_attribute(
		val attribute_name_index : Int,
		val classes : InnerClassesEntries
	) extends de.tud.cs.st.bat.canonical.InnerClasses_attribute	{

		type Class = BasicJava6Framework.InnerClassesEntry

	}

	def InnerClasses_attribute(
		attribute_name_index : Int, classes : InnerClassesEntries
	)( implicit constant_pool : Constant_Pool) : InnerClasses_attribute = 
		new InnerClasses_attribute( attribute_name_index, classes ) 


	case class InnerClassesEntry( 
		val inner_class_info_index : Int,
		val outer_class_info_index : Int,
		val inner_name_index : Int,
		val inner_class_access_flags : Int
	) extends de.tud.cs.st.bat.canonical.InnerClassesEntry	

	def InnerClassesEntry(
		inner_class_info_index : Int, outer_class_info_index : Int,
		inner_name_index : Int,	inner_class_access_flags : Int	
	)( implicit constant_pool : Constant_Pool) = 
		new InnerClassesEntry (
			inner_class_info_index, outer_class_info_index, 
			inner_name_index, 
			inner_class_access_flags 
		) 


	case class Signature_attribute(
		val attribute_name_index : Int,
		val signature_index : Int
	) extends de.tud.cs.st.bat.canonical.Signature_attribute 

	def Signature_attribute (
		attribute_name_index : Int,signature_index : Int
	)( implicit constant_pool : Constant_Pool) = 
		new Signature_attribute (attribute_name_index, signature_index)


	case class ConstantValue_attribute (
		val attribute_name_index : Int,
		val constantvalue_index : Int 
	) extends de.tud.cs.st.bat.canonical.ConstantValue_attribute

	def ConstantValue_attribute (
		attribute_name_index : Int, constantvalue_index : Int 
	)( implicit constant_pool : Constant_Pool) = new ConstantValue_attribute (attribute_name_index, constantvalue_index) 


	case class Deprecated_attribute (
		val attribute_name_index : Int
	) extends de.tud.cs.st.bat.canonical.Deprecated_attribute

	def Deprecated_attribute(attribute_name_index: Int)(implicit constant_pool : Constant_Pool) =
		new Deprecated_attribute(attribute_name_index)


	case class Synthetic_attribute (
		val attribute_name_index : Int
	) extends de.tud.cs.st.bat.canonical.Synthetic_attribute

	def Synthetic_attribute (attribute_name_index : Int)( implicit constant_pool : Constant_Pool) =
 		new Synthetic_attribute (attribute_name_index)


	case class SourceDebugExtension_attribute (
		val attribute_name_index : Int,
		val debug_extension : String
	) extends de.tud.cs.st.bat.canonical.SourceDebugExtension_attribute 

	def SourceDebugExtension_attribute (
		attribute_name_index : Int, attribute_length : Int, debug_extension : String
	)( implicit constant_pool : Constant_Pool) : SourceDebugExtension_attribute =
		new SourceDebugExtension_attribute (
			attribute_name_index, debug_extension
		) 


	case class Exceptions_attribute (
		val attribute_name_index : Int,
		val exception_index_table : ExceptionIndexTable
	) extends de.tud.cs.st.bat.canonical.Exceptions_attribute

	def Exceptions_attribute (
		attribute_name_index : Int, attribute_length : Int,
		exception_index_table : ExceptionIndexTable	
	)( implicit constant_pool : Constant_Pool) : Exceptions_attribute = 
		new Exceptions_attribute(attribute_name_index, exception_index_table)	


	case class EnclosingMethod_attribute(
		val attribute_name_index : Int,
		val class_index : Int,
		val method_index : Int
	) extends de.tud.cs.st.bat.canonical.EnclosingMethod_attribute

	def EnclosingMethod_attribute(
		attribute_name_index : Int, class_index : Int, method_index : Int
	)( implicit constant_pool : Constant_Pool) : EnclosingMethod_attribute = 
		new EnclosingMethod_attribute(
			attribute_name_index, class_index, method_index
		)


	case class LineNumberTable_attribute(
		val attribute_name_index : Int,
		val line_number_table : LineNumberTable		
	) extends de.tud.cs.st.bat.canonical.LineNumberTable_attribute {

		type LineNumberTableEntry = BasicJava6Framework.LineNumberTableEntry

	}

	def LineNumberTable_attribute (
		attribute_name_index : Int, attribute_length : Int, line_number_table : LineNumberTable
	)( implicit constant_pool : Constant_Pool) : LineNumberTable_attribute =
		new LineNumberTable_attribute (
			attribute_name_index, line_number_table
		) 


 	case class LineNumberTableEntry (
		val start_pc : Int,
		val line_number : Int
	) extends de.tud.cs.st.bat.canonical.LineNumberTableEntry 

	def LineNumberTableEntry (start_pc : Int, line_number : Int)( implicit constant_pool : Constant_Pool) = 
		new LineNumberTableEntry (start_pc, line_number)


	case class LocalVariableTableEntry(
		val start_pc : Int,
		val length : Int,
		val name_index : Int,
		val descriptor_index : Int,
		val index : Int
	) extends de.tud.cs.st.bat.canonical.LocalVariableTableEntry

	def LocalVariableTableEntry (
		start_pc : Int, length : Int,	name_index : Int,	descriptor_index : Int,	index : Int
	)( implicit constant_pool : Constant_Pool) : LocalVariableTableEntry = 
		new LocalVariableTableEntry (
			start_pc, length,	name_index,	descriptor_index,	index
		) 


	case class LocalVariableTable_attribute (
		val attribute_name_index : Int,
		val local_variable_table : LocalVariableTable
	) extends de.tud.cs.st.bat.canonical.LocalVariableTable_attribute {

		type LocalVariableTableEntry = BasicJava6Framework.LocalVariableTableEntry

	}

	def LocalVariableTable_attribute (	
		attribute_name_index : Int, attribute_length : Int, local_variable_table : LocalVariableTable
	)( implicit constant_pool : Constant_Pool) : LocalVariableTable_attribute =
		new LocalVariableTable_attribute (	
			attribute_name_index, local_variable_table
		) 


	case class LocalVariableTypeTableEntry(
		val start_pc : Int,
		val length : Int,
		val name_index : Int,
		val signature_index : Int,
		val index : Int
	) extends de.tud.cs.st.bat.canonical.LocalVariableTypeTableEntry

	def LocalVariableTypeTableEntry (
		start_pc : Int, length : Int,	name_index : Int,	signature_index : Int,	index : Int
	)( implicit constant_pool : Constant_Pool) : LocalVariableTypeTableEntry = 
		new LocalVariableTypeTableEntry (
			start_pc, length,	name_index,	signature_index,	index
		) 


	case class LocalVariableTypeTable_attribute (
		val attribute_name_index : Int,
		val local_variable_type_table : LocalVariableTypeTable
	) extends de.tud.cs.st.bat.canonical.LocalVariableTypeTable_attribute {

		type LocalVariableTypeTableEntry = BasicJava6Framework.LocalVariableTypeTableEntry

	}

	def LocalVariableTypeTable_attribute (
		attribute_name_index : Int, attribute_length : Int, 
		local_variable_type_table : LocalVariableTypeTable
	)( implicit constant_pool : Constant_Pool) =
 		new LocalVariableTypeTable_attribute (	
			attribute_name_index, local_variable_type_table
		) 


	case class Code ( val code : Array[Byte] )
	def Code(code : Array[Byte])(implicit constant_pool : Constant_Pool) : Code =
	  new Code(code)


	case class Code_attribute (
		val attribute_name_index : Int,
		val attribute_length : Int,
		val max_stack : Int,
		val max_locals : Int,
		val code : Code,
		val exception_table : ExceptionTable,
		val attributes : Attributes
	) extends de.tud.cs.st.bat.canonical.Code_attribute {

		//
		// ABSTRACT DEFINITIONS
		// 
		type Code = BasicJava6Framework.Code
		type ExceptionTableEntry = BasicJava6Framework.ExceptionTableEntry
		type Attributes = BasicJava6Framework.Attributes

	}

	def Code_attribute (
		attribute_name_index : Int, attribute_length : Int,
		max_stack : Int, max_locals : Int, code : Code,
		exception_table : ExceptionTable,
		attributes : Attributes
	)( implicit constant_pool : Constant_Pool) =
		new Code_attribute (
			attribute_name_index, attribute_length, max_stack, max_locals, code, exception_table, attributes
		)


	case class ExceptionTableEntry  (
		val start_pc: Int,
		val end_pc: Int,
		val handler_pc : Int,
		val catch_type : Int
	) extends de.tud.cs.st.bat.canonical.ExceptionTableEntry		

	def ExceptionTableEntry ( 
		start_pc: Int, end_pc: Int, handler_pc : Int, catch_type : Int
	)( implicit constant_pool : Constant_Pool) : ExceptionTableEntry =
		new ExceptionTableEntry (start_pc, end_pc, handler_pc, catch_type)


	case class ElementValuePair (
		val element_name_index : Int,
		val element_value : ElementValue
	) extends de.tud.cs.st.bat.canonical.ElementValuePair {

		type ElementValue = BasicJava6Framework.ElementValue
	}

	def ElementValuePair(
		element_name_index : Int,element_value : ElementValue
	)( implicit constant_pool : Constant_Pool) : ElementValuePair = 
		new ElementValuePair(element_name_index,element_value)



	case class ByteValue(val const_value_index : Int) extends de.tud.cs.st.bat.canonical.ByteValue
	def ByteValue(const_value_index : Int)(implicit constant_pool : Constant_Pool) : ElementValue = 
		new ByteValue(const_value_index)


	case class CharValue(val const_value_index : Int) extends de.tud.cs.st.bat.canonical.CharValue
	def CharValue(const_value_index : Int)(implicit constant_pool : Constant_Pool) : ElementValue = 
		new CharValue(const_value_index)


	case class DoubleValue(val const_value_index : Int) extends de.tud.cs.st.bat.canonical.DoubleValue
	def DoubleValue(const_value_index : Int)(implicit constant_pool : Constant_Pool) : ElementValue = 
		new DoubleValue(const_value_index)	


	case class FloatValue(val const_value_index : Int) extends de.tud.cs.st.bat.canonical.FloatValue
	def FloatValue(const_value_index : Int)(implicit constant_pool : Constant_Pool) : ElementValue = 
		new FloatValue(const_value_index)


	case class IntValue(val const_value_index : Int) extends de.tud.cs.st.bat.canonical.IntValue
	def IntValue(const_value_index : Int)(implicit constant_pool : Constant_Pool) : ElementValue = 
		new IntValue(const_value_index)


	case class LongValue(val const_value_index : Int) extends de.tud.cs.st.bat.canonical.LongValue
	def LongValue(const_value_index : Int)(implicit constant_pool : Constant_Pool) : ElementValue = 
		new LongValue(const_value_index)


	case class ShortValue(val const_value_index : Int) extends de.tud.cs.st.bat.canonical.ShortValue
	def ShortValue(const_value_index : Int)(implicit constant_pool : Constant_Pool) : ElementValue = 
		new ShortValue(const_value_index)


	case class BooleanValue(val const_value_index : Int) extends de.tud.cs.st.bat.canonical.BooleanValue
	def BooleanValue(const_value_index : Int)(implicit constant_pool : Constant_Pool) : ElementValue = 
		new BooleanValue(const_value_index)


	case class StringValue(val const_value_index : Int) extends de.tud.cs.st.bat.canonical.StringValue
	def StringValue(const_value_index : Int)(implicit constant_pool : Constant_Pool) : ElementValue = 
		new StringValue(const_value_index)


	case class ClassValue(val const_value_index : Int) extends de.tud.cs.st.bat.canonical.ClassValue
	def ClassValue(const_value_index : Int)(implicit constant_pool : Constant_Pool) : ElementValue = 
		new ClassValue(const_value_index)


	case class EnumValue(
		val type_name_index : Int,
		val const_name_index : Int
	) extends de.tud.cs.st.bat.canonical.EnumValue

	def EnumValue(
		type_name_index : Int,const_name_index : Int
	)( implicit constant_pool : Constant_Pool) : ElementValue = 
		new EnumValue(type_name_index,const_name_index)


	case class AnnotationValue (
		val annotation : Annotation
	) extends de.tud.cs.st.bat.canonical.AnnotationValue {

		type Annotation = BasicJava6Framework.Annotation

	}

	def AnnotationValue(annotation : Annotation)( implicit constant_pool : Constant_Pool) : ElementValue = 
		new AnnotationValue(annotation)


	case class ArrayValue (val values : Seq[ElementValue]) extends de.tud.cs.st.bat.canonical.ArrayValue
	def ArrayValue(values : ElementValues)( implicit constant_pool : Constant_Pool) : ElementValue = 
		new ArrayValue(values)


	case class Annotation ( val type_index : Int, val element_value_pairs : ElementValuePairs ) 
	extends de.tud.cs.st.bat.canonical.Annotation {

		type ElementValuePairs = BasicJava6Framework.ElementValuePairs

	}

	def Annotation (
		type_index : Int, element_value_pairs : ElementValuePairs
	)( implicit constant_pool : Constant_Pool) : Annotation = 
		new Annotation (type_index, element_value_pairs)


	case class AnnotationDefault_attribute (
		val attribute_name_index : Int,
		val attribute_length : Int,
		val element_value : ElementValue	
	) extends de.tud.cs.st.bat.canonical.AnnotationDefault_attribute {

		type ElementValue = BasicJava6Framework.ElementValue

	}

	def AnnotationDefault_attribute (
		attribute_name_index : Int, attribute_length : Int, element_value : ElementValue
	)( implicit constant_pool : Constant_Pool) =
		new AnnotationDefault_attribute(
			attribute_name_index,attribute_length,element_value
		)


	case class RuntimeVisibleAnnotations_attribute (
		val attribute_name_index : Int,
		val attribute_length : Int,
		val annotations : Annotations
	) extends de.tud.cs.st.bat.canonical.RuntimeVisibleAnnotations_attribute {

		type Annotations = BasicJava6Framework.Annotations
	}

	def RuntimeVisibleAnnotations_attribute (
		attribute_name_index : Int, attribute_length : Int,	annotations : Annotations
	)( implicit constant_pool : Constant_Pool) =	
		new RuntimeVisibleAnnotations_attribute (
			attribute_name_index, attribute_length,	annotations
		) 


	case class RuntimeInvisibleAnnotations_attribute (
		val attribute_name_index : Int,
		val attribute_length : Int,
		val annotations : Annotations
	) extends de.tud.cs.st.bat.canonical.RuntimeInvisibleAnnotations_attribute {

		type Annotations = BasicJava6Framework.Annotations
	}

	def RuntimeInvisibleAnnotations_attribute (
		attribute_name_index : Int, attribute_length : Int, annotations : Annotations
	)( implicit constant_pool : Constant_Pool) =	
		new RuntimeInvisibleAnnotations_attribute (
			attribute_name_index, attribute_length,	annotations
		)


	case class RuntimeVisibleParameterAnnotations_attribute (
		val attribute_name_index : Int, val attribute_length : Int,
		val parameter_annotations : ParameterAnnotations
	)
	extends de.tud.cs.st.bat.canonical.RuntimeVisibleParameterAnnotations_attribute  {

		type ParameterAnnotations = BasicJava6Framework.ParameterAnnotations

	}

	def RuntimeVisibleParameterAnnotations_attribute (
		attribute_name_index : Int, attribute_length : Int, parameter_annotations : ParameterAnnotations
	)( implicit constant_pool : Constant_Pool) =
		new RuntimeVisibleParameterAnnotations_attribute(
			attribute_name_index,attribute_length,parameter_annotations
		)


	case class RuntimeInvisibleParameterAnnotations_attribute (
		val attribute_name_index : Int, val attribute_length : Int,
		val parameter_annotations : ParameterAnnotations
	)
	extends de.tud.cs.st.bat.canonical.RuntimeInvisibleParameterAnnotations_attribute  {

		type ParameterAnnotations = BasicJava6Framework.ParameterAnnotations

	}

	def RuntimeInvisibleParameterAnnotations_attribute (
		attribute_name_index : Int, attribute_length : Int,parameter_annotations : ParameterAnnotations
	)( implicit constant_pool : Constant_Pool) =
		new RuntimeInvisibleParameterAnnotations_attribute(
			attribute_name_index,attribute_length,parameter_annotations
		)


	case class StackMapTable_attribute (
		val attribute_name_index : Int,
		val attribute_length : Int,
		val stack_map_frames : StackMapFrames
	) extends de.tud.cs.st.bat.canonical.StackMapTable_attribute {

		type StackMapFrames = BasicJava6Framework.StackMapFrames

	}
	
	def StackMapTable_attribute (
		attribute_name_index : Int, attribute_length : Int, stack_map_frames : StackMapFrames
	)( implicit constant_pool : Constant_Pool) =
		new StackMapTable_attribute (attribute_name_index, attribute_length, stack_map_frames )


	trait StackMapFrame extends de.tud.cs.st.bat.canonical.StackMapFrame {
		type VerificationTypeInfo = BasicJava6Framework.VerificationTypeInfo
	}


	case class SameFrame(val frame_type : Int) extends de.tud.cs.st.bat.canonical.SameFrame with StackMapFrame
	def SameFrame(frame_type : Int) : StackMapFrame = new SameFrame(frame_type)


	case class SameLocals1StackItemFrame(val frame_type : Int, val verification_type_info_stack : VerificationTypeInfo) extends de.tud.cs.st.bat.canonical.SameLocals1StackItemFrame with StackMapFrame
	def SameLocals1StackItemFrame(
		frame_type: Int,verification_type_info_stack : VerificationTypeInfo
	) : StackMapFrame = new SameLocals1StackItemFrame(frame_type,verification_type_info_stack)


	case class SameLocals1StackItemFrameExtended(val frame_type : Int, val offset_delta : Int, val verification_type_info_stack : VerificationTypeInfo) extends de.tud.cs.st.bat.canonical.SameLocals1StackItemFrameExtended with StackMapFrame
	def SameLocals1StackItemFrameExtended(
		frame_type: Int,
		offset_delta: Int,
		verification_type_info_stack : VerificationTypeInfo
	) : StackMapFrame = new SameLocals1StackItemFrameExtended(frame_type,offset_delta,verification_type_info_stack)


	case class ChopFrame(val frame_type : Int, val offset_delta : Int) extends de.tud.cs.st.bat.canonical.ChopFrame with StackMapFrame
	def ChopFrame(frame_type: Int, offset_delta : Int ) : StackMapFrame = new ChopFrame(frame_type,offset_delta)


	case class SameFrameExtended(val frame_type : Int, val offset_delta : Int) extends de.tud.cs.st.bat.canonical.SameFrameExtended with StackMapFrame
	def SameFrameExtended( frame_type: Int, offset_delta : Int) : StackMapFrame = new SameFrameExtended(frame_type,offset_delta)


	case class AppendFrame(val frame_type : Int, val offset_delta : Int, val verification_type_info_locals : VerificationTypeInfoLocals) extends de.tud.cs.st.bat.canonical.AppendFrame with StackMapFrame	
	def AppendFrame(
		frame_type: Int,	
		offset_delta: Int, verification_type_info_locals : VerificationTypeInfoLocals
	) : StackMapFrame = new AppendFrame(frame_type,offset_delta,verification_type_info_locals)


	case class FullFrame(val frame_type : Int, val offset_delta : Int, val verification_type_info_locals : VerificationTypeInfoLocals, val verification_type_info_stack : VerificationTypeInfoStack) extends de.tud.cs.st.bat.canonical.FullFrame with StackMapFrame		
	def FullFrame(
		frame_type: Int,	
		offset_delta: Int,
		verification_type_info_locals : VerificationTypeInfoLocals,
		verification_type_info_stack : VerificationTypeInfoStack
	) : StackMapFrame = new FullFrame(frame_type,offset_delta,verification_type_info_locals,verification_type_info_stack)


	type VerificationTypeInfo = de.tud.cs.st.bat.canonical.VerificationTypeInfo

	case object TopVariableInfo extends de.tud.cs.st.bat.canonical.TopVariableInfo 
	def TopVariableInfo () : VerificationTypeInfo = TopVariableInfo

	case object IntegerVariableInfo extends de.tud.cs.st.bat.canonical.IntegerVariableInfo
	def IntegerVariableInfo () : VerificationTypeInfo = IntegerVariableInfo

	case object FloatVariableInfo extends de.tud.cs.st.bat.canonical.FloatVariableInfo
	def FloatVariableInfo () : VerificationTypeInfo = FloatVariableInfo

	case object LongVariableInfo extends de.tud.cs.st.bat.canonical.LongVariableInfo
	def LongVariableInfo () : VerificationTypeInfo = LongVariableInfo

	case object DoubleVariableInfo extends de.tud.cs.st.bat.canonical.DoubleVariableInfo
	def DoubleVariableInfo () : VerificationTypeInfo = DoubleVariableInfo

	case object NullVariableInfo extends de.tud.cs.st.bat.canonical.NullVariableInfo
	def NullVariableInfo () : VerificationTypeInfo = NullVariableInfo

	case object UninitializedThisVariableInfo extends de.tud.cs.st.bat.canonical.UninitializedThisVariableInfo
	def UninitializedThisVariableInfo () : VerificationTypeInfo = UninitializedThisVariableInfo

	case class UninitializedVariableInfo(val offset : Int) extends de.tud.cs.st.bat.canonical.UninitializedVariableInfo 
	def UninitializedVariableInfo(offset : Int) : VerificationTypeInfo = 
		new UninitializedVariableInfo(offset)
		
	case class ObjectVariableInfo(val cpool_index : Int) extends de.tud.cs.st.bat.canonical.ObjectVariableInfo
	def ObjectVariableInfo(cpool_index : Int)(implicit constant_pool : Constant_Pool) : VerificationTypeInfo = 
		new ObjectVariableInfo(cpool_index)		
}


