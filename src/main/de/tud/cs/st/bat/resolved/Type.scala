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
package de.tud.cs.st.bat.resolved

import de.tud.cs.st.prolog.{GroundTerm,Atom,Fact}

/**
 * Representation of a JVM type.
 *
 * @author Michael Eichberg
 */
sealed trait Type {

	def isReturnType : Boolean = true

	def isFieldType : Boolean = false
	def isBaseType : Boolean = false
	def isReferenceType : Boolean = false

	def isVoidType : Boolean = false
	def isByteType : Boolean = false
	def isCharType : Boolean = false
	def isShortType : Boolean = false
	def isIntegerType : Boolean = false
	def isLongType : Boolean = false
	def isFloatType : Boolean = false
	def isDoubleType : Boolean = false
	def isBooleanType : Boolean = false
	def isArrayType : Boolean = false
	def isObjectType : Boolean = false

	def toJava : String

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) : T
}


trait ReturnType extends Type {

	override final def isReturnType = true
}
object ReturnType  {

	def apply( rt : String ) : ReturnType = {
		rt match {
			case "V" => VoidType()
			case _ => FieldType.apply(rt)
		}
	}
}


final class VoidType extends ReturnType {

	// remark: the default implementation of equals and hashCode suits our needs!

	override def isVoidType = true

	def toJava : String = "void"

	override def toString() = "VoidType"

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) = factory.StringAtom("void")

}
object VoidType {

	private val vt = new VoidType

	def apply() = vt
}



trait FieldType extends ReturnType {

	override final def isFieldType = true
}
/**
 * Factory object to parse field type (descriptors) to get field type objects.
 */
object FieldType {

	private val ObjectTypeRegEx = """L([\S&&[^;]]+);""".r

	private val ArrayTypeRegEx = """\[(.+)""".r

	def apply( ft : String ) : FieldType = {
		ft match {
			case "B" => ByteType()
			case "C" => CharType()
			case "D" => DoubleType()
			case "F" => FloatType()
			case "I" => IntegerType()
			case "J" => LongType()
			case "S" => ShortType()
			case "Z" => BooleanType()
			case ObjectTypeRegEx(classname) => ObjectType(classname)
			case ArrayTypeRegEx(componentTypeDescriptor) => {
				val componentType = FieldType(componentTypeDescriptor)
				ArrayType(componentType)
			}
		}
	}
}



trait BaseType extends FieldType {

	override final def isBaseType = true
}



trait ReferenceType extends FieldType {

	override final def isReferenceType = true
}



final class ByteType private () extends BaseType {

	override def isByteType = true

	def toJava : String = "byte"

	override def toString() = "ByteType"

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) = factory.StringAtom("byte")
}
object ByteType {

	private val bt = new ByteType;

	def apply () = bt
}



final class CharType private () extends BaseType  {

	override def isCharType = true

	def toJava : String = "char"

	override def toString() = "CharType"

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) = factory.StringAtom("char")
}
object CharType {

	private val ct = new CharType

	def apply () = ct
}



final class DoubleType private () extends BaseType {

	override def isDoubleType = true

	def toJava : String = "double"

	override def toString() = "DoubleType"

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) = factory.StringAtom("double")
}
object DoubleType {

	private val dt = new DoubleType

	def apply () = dt
}



final class FloatType private () extends BaseType {

	override def isFloatType = true

	def toJava : String = "float"

	override def toString() = "FloatType"

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) = factory.StringAtom("float")
}
object FloatType {

	private val ft = new FloatType

	def apply () = ft
}



final class ShortType private () extends BaseType  {

	override def isShortType = true

	def toJava : String = "short"

	override def toString() = "ShortType"

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) = factory.StringAtom("short")
}
object ShortType {

	private val st = new ShortType

	def apply () = st
}



final class IntegerType private () extends BaseType {

	override def isIntegerType = true

	def toJava : String = "int"

	override def toString() = "IntegerType"

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) = factory.StringAtom("int")
}
object IntegerType {

	private val it = new IntegerType

	def apply () = it
}



final class LongType private () extends BaseType  {

	override def isLongType = true

	def toJava : String = "long"

	override def toString() = "LongType"

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) = factory.StringAtom("long")
}
object LongType {

	private val lt = new LongType

	def apply () = lt

}



final class BooleanType private () extends BaseType {

	override def isBooleanType = true

	def toJava : String = "boolean"

	override def toString() = "BooleanType"

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) = factory.StringAtom("boolean")
}
object BooleanType {

	private val bt = new BooleanType

	def apply () = bt
}



class ObjectType private(
	val className : String
) extends ReferenceType {

	override final def isObjectType = true

	override def hashCode = className.hashCode * 43

	override def equals (other : Any) : Boolean = {
		other match {
			case that : ObjectType => that.className == this.className
			case _ => false
		}
	}

	def simpleName : String = ObjectType.simpleName(className)

	def packageName : String = ObjectType.packageName(className)

	def toJava : String = className.replace('/','.')

	override def toString = "ObjectType(className=\""+className+"\")"

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) =
		factory.Term("class",factory.TextAtom(packageName),factory.TextAtom(simpleName))

}
object ObjectType {


	// FIXME memory leak...
	private val cache : scala.collection.mutable.Map[String,ObjectType] = scala.collection.mutable.Map()

	/**
 	 * Factory method to create ObjectTypes.<br />
	 * This method makes sure that every class is represented by exactly one object type.
	 */
	def apply (className : String) = {
		cache.getOrElseUpdate(className,new ObjectType(className))
	}

	def unapply (ot : ObjectType) : Option[String] = Some(ot.className)


	def simpleName (className : String) : String = {
		val index = className.lastIndexOf('/')
		if (index > -1)
			className.substring(index+1)
		else
			className
	}


	def packageName (className : String) : String = {
		val index = className.lastIndexOf('/')
		if (index == -1)
			""
		else
			className.substring(0,index)
	}
}



class ArrayType private ( val componentType : FieldType ) extends ReferenceType {

	override final def isArrayType = true

	override def hashCode = 13 * (componentType.hashCode + 7)

	override def equals (other : Any) : Boolean = {
		other match {
			case that : ArrayType => this.componentType == that.componentType
			case _ => false
		}
	}

	def toJava : String = componentType.toJava+"[]"

	override def toString = "ArrayType("+componentType.toString+")"

	def toProlog[F,T,A <: T](factory : PrologTermFactory[F,T,A]) =
		factory.Term("array",componentType.toProlog(factory))

}
object ArrayType {

	// FIXME memory leak...
	private val cache : scala.collection.mutable.Map[FieldType,ArrayType] = scala.collection.mutable.Map()

	/**
 	 * Factory method to create objects of type <code>ArrayType</code>.
 	 *
	 * This method makes sure that every array type is represented by exactly one ArrayType object.
	 */
	def apply (componentType : FieldType) = {
		cache.getOrElseUpdate(componentType,new ArrayType(componentType))
	}

	def unapply (at : ArrayType) : Option[FieldType] = Some(at.componentType)

}






