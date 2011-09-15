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
package de.tud.cs.st.bat.native


/**
 * Enables iterating over a class( file member)'s access flags. 
 *
 * @author Michael Eichberg
 */
final class AccessFlagsIterator(accessFlags : Int, ctx : AccessFlagsContext.Value) extends Iterator[AccessFlag] {
	
	import AccessFlagsContext._
	
	private var flags = accessFlags
	
	private val potentialAccessFlags = {
		ctx match {
			case INNER_CLASS => INNER_CLASS_FLAGS
			case CLASS => CLASS_FLAGS
			case METHOD => METHOD_FLAGS
			case FIELD => FIELD_FLAGS
		}
	}
	
	private var index = -1
	
	
	def hasNext = flags != 0
	
	
	def next : AccessFlag = {
		while ((index+1) < potentialAccessFlags.size) {
			index += 1
			if ((flags & potentialAccessFlags(index).mask) != 0) {
				flags = flags & (~ potentialAccessFlags(index).mask)
				return potentialAccessFlags(index)
			}
		}
		sys.error("Unknown access flag(s): "+Integer.toHexString(flags))
	}
}
object AccessFlagsIterator {
	
	def apply(accessFlags : Int, ctx : AccessFlagsContext.Value) = new AccessFlagsIterator(accessFlags,ctx)
	
}

/**  
 * The semantics of the "access_flags" bit mask is dependent on its context. This class models the different contexts
 * that exist and defines which access_flags can be found in which context.
 *
 * @author Michael Eichberg
 */
object AccessFlagsContext extends Enumeration {
	
	val INNER_CLASS, CLASS, METHOD, FIELD = Value

	val INNER_CLASS_FLAGS : IndexedSeq[AccessFlag] = Array(ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED, ACC_STATIC, ACC_SUPER/*NOT SPECIFIED IN THE JVM SPEC. - MAYBE THIS BIT IS JUST SET BY THE SCALA COMPILER!*/, ACC_FINAL, ACC_INTERFACE, ACC_ABSTRACT, ACC_SYNTHETIC, ACC_ANNOTATION, ACC_ENUM)
	val CLASS_FLAGS : IndexedSeq[AccessFlag] = Array(ACC_PUBLIC, ACC_FINAL, ACC_SUPER, ACC_INTERFACE, ACC_ABSTRACT, ACC_SYNTHETIC, ACC_ANNOTATION, ACC_ENUM)
	val FIELD_FLAGS : IndexedSeq[AccessFlag] = Array(ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED, ACC_STATIC, ACC_FINAL, ACC_VOLATILE, ACC_TRANSIENT, ACC_SYNTHETIC, ACC_ENUM)
	val METHOD_FLAGS : IndexedSeq[AccessFlag] = Array(ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED, ACC_STATIC, ACC_FINAL, ACC_SYNCHRONIZED, ACC_BRIDGE, ACC_VARARGS, ACC_NATIVE, ACC_ABSTRACT, ACC_STRICT, ACC_SYNTHETIC)

	val CLASS_VISIBILITY_FLAGS : IndexedSeq[AccessFlag] = Array(ACC_PUBLIC)	
	val MEMBER_VISIBILITY_FLAGS : IndexedSeq[AccessFlag] = Array(ACC_PUBLIC, ACC_PRIVATE, ACC_PROTECTED)
	val INNER_CLASS_VISIBILITY_FLAGS : IndexedSeq[AccessFlag] = MEMBER_VISIBILITY_FLAGS
	val FIELD_VISIBILITY_FLAGS : IndexedSeq[AccessFlag] = MEMBER_VISIBILITY_FLAGS
	val METHOD_VISIBILITY_FLAGS : IndexedSeq[AccessFlag] = MEMBER_VISIBILITY_FLAGS
	
}

/**
 * Common super type of all access flags.
 *
 * @author Michael Eichberg
 */
object AccessFlags {
		
	import scala.xml._
	
	def toXML(flags : Int, ctx : AccessFlagsContext.Value) = 
		<flags>{ AccessFlagsIterator(flags, ctx) map ( (f) => Elem(null,f.toString,Null,TopScope) ) }</flags>
	
}

/**
 * Common super type of all access flags.
 *
 * @author Michael Eichberg
 */
sealed trait AccessFlag {
	
	def javaName : String
	
	def mask : Int 
	
	def element_of (access_flags : Int) : Boolean = (access_flags & mask) != 0
	
	final def ∈ (access_flags : Int) : Boolean = element_of (access_flags)
}
case object ACC_PUBLIC extends AccessFlag {
	// CONTEXT: CLASS, FIELD, METHOD
	val javaName = "public"
	val mask = 0x0001
	override def toString = "public"	
}
case object ACC_PRIVATE extends AccessFlag {
	// CONTEXT: FIELD, METHOD
	val javaName = "private"
	val mask = 0x0002
	override def toString = "private"	
}
case object ACC_PROTECTED extends AccessFlag {
	// CONTEXT: FIELD, METHOD
	val javaName = "protected"
	val mask = 0x0004
	override def toString = "protected"	
}
case object ACC_STATIC extends AccessFlag {
	// CONTEXT: FIELD, METHOD
	val javaName = "static"
	val mask = 0x0008
	override def toString = "static"	
}
case object ACC_FINAL extends AccessFlag {
	// CONTEXT: CLASS, FIELD, METHOD	
	val javaName = "final"
	val mask = 0x0010
	override def toString = "final"	
}
case object ACC_SUPER extends AccessFlag {
	// CONTEXT: CLASS
	val javaName = null
	val mask = 0x0020
	override def toString = "super"	
}
case object ACC_SYNCHRONIZED extends AccessFlag {
	// CONTEXT: METHOD
	val javaName = "synchronized"
	val mask = 0x0020
	override def toString = "synchronized"	
}
case object ACC_VOLATILE extends AccessFlag {
	// CONTEXT: FIELD
	val javaName = "volatile"
	val mask = 0x0040
	override def toString = "volatile"	
}
case object ACC_BRIDGE extends AccessFlag {
	// CONTEXT: METHOD
	val javaName = null
	val mask = 0x0040
	override def toString = "bridge"	
}
case object ACC_TRANSIENT extends AccessFlag {
	// CONTEXT: FIELD
	val javaName = "transient"
	val mask = 0x0080
	override def toString = "transient"	
}
case object ACC_VARARGS extends AccessFlag {
	// CONTEXT: METHOD
	val javaName = null
	val mask = 0x0080
	override def toString = "varags"	
}
case object ACC_NATIVE extends AccessFlag {
	// CONTEXT: METHOD
	val javaName = "native"
	val mask = 0x0100
	override def toString = "native"	
}
case object ACC_INTERFACE extends AccessFlag {
	// CONTEXT: CLASS
	val javaName = null // this flag modifies the semantics of a class, but it is not an additional flag
	val mask = 0x0200
	override def toString = "interface"	
}
case object ACC_ABSTRACT extends AccessFlag {
	// CONTEXT: CLASS	
	val javaName = "abstract"
	val mask = 0x0400
	override def toString = "abstract"	
}
case object ACC_STRICT extends AccessFlag {
	// CONTEXT: METHOD
	val javaName = "strictfp"
	val mask = 0x0800
	override def toString = "strictfp"	
}
case object ACC_SYNTHETIC extends AccessFlag {
	// CONTEXT: CLASS, FIELD, METHOD
	val javaName = null
	val mask = 0x1000
	override def toString = "synthetic"	
}
case object ACC_ANNOTATION extends AccessFlag {
	// CONTEXT: CLASS	
	val javaName = null
	val mask = 0x2000
	override def toString = "annotation"	
}
case object ACC_ENUM extends AccessFlag {
	// CONTEXT: CLASS, FIELD	
	val javaName = null
	val mask = 0x4000
	override def toString = "enum"	
}

