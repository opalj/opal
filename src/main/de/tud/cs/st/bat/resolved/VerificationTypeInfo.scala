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

/**
 * Part of the Java 6 stack map table attribute.
 *
 * @author Michael Eichberg
 */
sealed trait VerificationTypeInfo {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toXML: scala.xml.Node

}

case object TopVariableInfo extends VerificationTypeInfo {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toXML = <top_variable/>
}

case object IntegerVariableInfo extends VerificationTypeInfo {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toXML = <integer_variable/>
}

case object FloatVariableInfo extends VerificationTypeInfo {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toXML = <float_variable/>
}

case object LongVariableInfo extends VerificationTypeInfo {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toXML = <long_variable/>
}

case object DoubleVariableInfo extends VerificationTypeInfo {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toXML = <double_variable/>
}

case object NullVariableInfo extends VerificationTypeInfo {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toXML = <null_variable/>
}

case object UninitializedThisVariableInfo extends VerificationTypeInfo {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toXML = <uninitialized_this_variable/>
}

case class UninitializedVariableInfo(offset: Int) extends VerificationTypeInfo {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toXML = <uninitialized_variable offset={ offset.toString }/>
}

case class ObjectVariableInfo(clazz: ReferenceType) extends VerificationTypeInfo {

    //
    //
    // SUPPORT FOR SPECIAL REPRESENTATIONS
    //
    //

    def toXML = <object_variable type={ clazz.toJava }/>
}
