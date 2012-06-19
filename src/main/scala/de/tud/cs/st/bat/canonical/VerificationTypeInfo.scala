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
package de.tud.cs.st.bat.canonical

/**
 *
 * @author Michael Eichberg
 */
trait VerificationTypeInfo {

    //
    // ABSTRACT DEFINITIONS
    //

    def tag: Int
}

import de.tud.cs.st.bat.reader.VerificationTypeInfoItem._

trait TopVariableInfo extends VerificationTypeInfo {
    def tag = ITEM_Top.id
}

trait IntegerVariableInfo extends VerificationTypeInfo {
    def tag = ITEM_Integer.id
}

trait FloatVariableInfo extends VerificationTypeInfo {
    def tag = ITEM_Float.id
}

trait LongVariableInfo extends VerificationTypeInfo {
    def tag = ITEM_Long.id
}

trait DoubleVariableInfo extends VerificationTypeInfo {
    def tag = ITEM_Double.id
}

trait NullVariableInfo extends VerificationTypeInfo {
    def tag = ITEM_Null.id
}

trait UninitializedThisVariableInfo extends VerificationTypeInfo {
    def tag = ITEM_UninitializedThis.id
}

trait ObjectVariableInfo extends VerificationTypeInfo {

    val cpool_index: Int

    def tag = ITEM_Object.id
}

trait UninitializedVariableInfo extends VerificationTypeInfo {

    val offset: Int

    def tag = ITEM_Unitialized.id
}


