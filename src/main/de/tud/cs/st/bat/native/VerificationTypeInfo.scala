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

 * @author Michael Eichberg
 */
trait VerificationTypeInfo {
	
	//
	// ABSTRACT DEFINITIONS
	//
	
	val tag : Int
}


object VerificationTypeInfo {
	val ITEM_Top = 0
	val ITEM_Integer = 1
	val ITEM_Float = 2
	val ITEM_Long = 4
	val ITEM_Double = 3
	val ITEM_Null = 5
	val ITEM_UninitializedThis = 6
	val ITEM_Object = 7
	val ITEM_Unitialized = 8
}


trait TopVariableInfo extends VerificationTypeInfo {
	val tag = VerificationTypeInfo.ITEM_Top
}


trait IntegerVariableInfo extends VerificationTypeInfo {
	val tag = VerificationTypeInfo.ITEM_Integer
}	


trait FloatVariableInfo extends VerificationTypeInfo {
	val tag = VerificationTypeInfo.ITEM_Float
}


trait LongVariableInfo extends VerificationTypeInfo {
	val tag = VerificationTypeInfo.ITEM_Long
}


trait DoubleVariableInfo extends VerificationTypeInfo{
	val tag = VerificationTypeInfo.ITEM_Double
}


trait NullVariableInfo extends VerificationTypeInfo {
	val tag = VerificationTypeInfo.ITEM_Null
}


trait UninitializedThisVariableInfo extends VerificationTypeInfo {
	val tag = VerificationTypeInfo.ITEM_UninitializedThis
}


trait ObjectVariableInfo extends VerificationTypeInfo{

	val cpool_index : Int
	
	val tag = VerificationTypeInfo.ITEM_Null
}


trait UninitializedVariableInfo extends VerificationTypeInfo {

	val offset : Int
	
	val tag = VerificationTypeInfo.ITEM_Null
}


