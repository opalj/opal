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
package de.tud.cs.st
package bat
package reader

import java.io.DataInputStream

import reflect.ClassTag

/**
 *
 * @author Michael Eichberg
 */
trait ElementValuePairsReader extends Constant_PoolAbstractions {

    //
    // ABSTRACT DEFINITIONS
    //

    type ElementValue
    implicit val ElementValueManifest: ClassTag[ElementValue]

    type ElementValuePair
    implicit val ElementValuePairManifest: ClassTag[ElementValuePair]

    type Annotation

    def Annotation(in: DataInputStream, cp: Constant_Pool): Annotation

    def ElementValuePair(
        element_name_index: Constant_Pool_Index,
        element_value: ElementValue)(
            implicit constant_pool: Constant_Pool): ElementValuePair

    def ByteValue(const_value_index: Constant_Pool_Index)(implicit constant_pool: Constant_Pool): ElementValue
    def CharValue(const_value_index: Constant_Pool_Index)(implicit constant_pool: Constant_Pool): ElementValue
    def DoubleValue(const_value_index: Constant_Pool_Index)(implicit constant_pool: Constant_Pool): ElementValue
    def FloatValue(const_value_index: Constant_Pool_Index)(implicit constant_pool: Constant_Pool): ElementValue
    def IntValue(const_value_index: Constant_Pool_Index)(implicit constant_pool: Constant_Pool): ElementValue
    def LongValue(const_value_index: Constant_Pool_Index)(implicit constant_pool: Constant_Pool): ElementValue
    def ShortValue(const_value_index: Constant_Pool_Index)(implicit constant_pool: Constant_Pool): ElementValue
    def BooleanValue(const_value_index: Constant_Pool_Index)(implicit constant_pool: Constant_Pool): ElementValue
    def StringValue(const_value_index: Constant_Pool_Index)(implicit constant_pool: Constant_Pool): ElementValue
    def ClassValue(const_value_index: Constant_Pool_Index)(implicit constant_pool: Constant_Pool): ElementValue
    def EnumValue(type_name_index: Constant_Pool_Index, const_name_index: Constant_Pool_Index)(implicit constant_pool: Constant_Pool): ElementValue
    def AnnotationValue(annotation: Annotation)(implicit constant_pool: Constant_Pool): ElementValue
    def ArrayValue(values: ElementValues)(implicit constant_pool: Constant_Pool): ElementValue

    //
    // IMPLEMENTATION
    //

    import util.ControlAbstractions.repeat

    type ElementValues = IndexedSeq[ElementValue]
    type ElementValuePairs = IndexedSeq[ElementValuePair]

    def ElementValuePairs(in: DataInputStream, cp: Constant_Pool): ElementValuePairs = {
        repeat(in.readUnsignedShort) {
            ElementValuePair(in, cp)
        }
    }

    def ElementValuePair(in: DataInputStream, cp: Constant_Pool): ElementValuePair =
        ElementValuePair(in.readUnsignedShort, ElementValue(in, cp))(cp)

    /**
     * Reads in an element value.
     *
     * '''From the Specification'''
     * <pre><code>
     * element_value {
     *    u1 tag;
     *    union {
     *      u2   const_value_index;
     *
     *      {
     *        u2 type_name_index;
     *        u2 const_name_index;
     *      } enum_const_value;
     *
     *      u2 class_info_index;
     *
     *      annotation annotation_value;
     *
     *      {
     *        u2    num_values;
     *        element_value values[num_values];
     *      } array_value;
     *    } value;
     * }
     * </code></pre>
     */
    def ElementValue(in: DataInputStream, cp: Constant_Pool): ElementValue = {
        val tag = in.readByte
        (tag: @scala.annotation.switch) match {
            case 'B' ⇒ ByteValue(in.readUnsignedShort)(cp)
            case 'C' ⇒ CharValue(in.readUnsignedShort)(cp)
            case 'D' ⇒ DoubleValue(in.readUnsignedShort)(cp)
            case 'F' ⇒ FloatValue(in.readUnsignedShort)(cp)
            case 'I' ⇒ IntValue(in.readUnsignedShort)(cp)
            case 'J' ⇒ LongValue(in.readUnsignedShort)(cp)
            case 'S' ⇒ ShortValue(in.readUnsignedShort)(cp)
            case 'Z' ⇒ BooleanValue(in.readUnsignedShort)(cp)
            case 's' ⇒ StringValue(in.readUnsignedShort)(cp)
            case 'e' ⇒ EnumValue(in.readUnsignedShort, in.readUnsignedShort)(cp)
            case 'c' ⇒ ClassValue(in.readUnsignedShort)(cp)
            case '@' ⇒ AnnotationValue(Annotation(in, cp))(cp)
            case '[' ⇒ ArrayValue(repeat(in.readUnsignedShort) { ElementValue(in, cp) })(cp)
        }
    }
}
